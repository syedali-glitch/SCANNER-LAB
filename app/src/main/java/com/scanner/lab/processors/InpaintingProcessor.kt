package com.scanner.lab.processors

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Kotlin Implementation of Alexandru Telea's Fast Marching Method (FMM) for Image Inpainting.
 * 
 * Simplified for Android Bitmap processing using Float logic to avoid compiler inference issues.
 */
object InpaintingProcessor {

    private const val UNKNOWN = 0
    private const val BAND = 1
    private const val KNOWN = 2
    private const val MAX_DISTANCE = 1e6f

    private data class Node(val dist: Float, val offset: Int) : Comparable<Node> {
        override fun compareTo(other: Node): Int = this.dist.compareTo(other.dist)
    }

    suspend fun inpaint(original: Bitmap, mask: Bitmap, radius: Int = 5): Bitmap = withContext(Dispatchers.Default) {
        val width = original.width
        val height = original.height

        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        val flags = IntArray(width * height)
        val t = FloatArray(width * height) { MAX_DISTANCE }
        
        val heap = PriorityQueue<Node>()

        // Initialize
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * width + x
                val maskPixel = mask.getPixel(x, y)
                // Assuming mask is red/white
                val isDamage = Color.red(maskPixel) > 128

                if (!isDamage) {
                    flags[offset] = KNOWN
                    t[offset] = 0f
                } else {
                    flags[offset] = UNKNOWN
                }
            }
        }

        // Find Boundary (BAND)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * width + x
                if (flags[offset] == UNKNOWN) {
                    val neighbors = getNeighbors(x, y, width, height)
                    for (n in neighbors) {
                        if (flags[n] == KNOWN) {
                            flags[offset] = BAND
                            t[offset] = 0f
                            heap.add(Node(0f, offset))
                            break
                        }
                    }
                }
            }
        }

        // Fast Marching
        while (heap.isNotEmpty()) {
            val node = heap.poll() ?: break
            val currentOffset = node.offset
            
            if (flags[currentOffset] == KNOWN) continue // Skip if processed/stale
            
            flags[currentOffset] = KNOWN // Mark as fixed

            val cx = currentOffset % width
            val cy = currentOffset / width

            // Propagate to neighbors
            val neighbors = getNeighbors(cx, cy, width, height)
            for (nOffset in neighbors) {
                if (flags[nOffset] != KNOWN) {
                    val nx = nOffset % width
                    val ny = nOffset / width
                    
                    val dist = solveEikonal(nx, ny, width, height, t, flags)
                    
                    if (flags[nOffset] == UNKNOWN) {
                        flags[nOffset] = BAND
                        t[nOffset] = dist
                        heap.add(Node(dist, nOffset))
                        
                        val color = solveInpaint(nx, ny, width, height, t, flags, pixels, radius)
                        pixels[nOffset] = color
                    } else {
                        // BAND: Update if smaller
                        if (dist < t[nOffset]) {
                            t[nOffset] = dist
                            heap.add(Node(dist, nOffset))
                            
                            val color = solveInpaint(nx, ny, width, height, t, flags, pixels, radius)
                            pixels[nOffset] = color
                        }
                    }
                }
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return@withContext result
    }

    private fun getNeighbors(x: Int, y: Int, width: Int, height: Int): List<Int> {
        val list = ArrayList<Int>(4)
        if (x > 0) list.add(y * width + (x - 1))
        if (x < width - 1) list.add(y * width + (x + 1))
        if (y > 0) list.add((y - 1) * width + x)
        if (y < height - 1) list.add((y + 1) * width + x)
        return list
    }

    // Solve |grad T| = 1
    private fun solveEikonal(x: Int, y: Int, width: Int, height: Int, t: FloatArray, flags: IntArray): Float {
        var t1 = MAX_DISTANCE
        
        // Horizontal
        if (x > 0 && flags[y * width + (x - 1)] == KNOWN) {
            val v = t[y * width + (x - 1)]
            if (v < t1) t1 = v
        }
        if (x < width - 1 && flags[y * width + (x + 1)] == KNOWN) {
             val v = t[y * width + (x + 1)]
             if (v < t1) t1 = v
        }
        
        // Vertical
        var t2 = MAX_DISTANCE
        if (y > 0 && flags[(y - 1) * width + x] == KNOWN) {
             val v = t[(y - 1) * width + x]
             if (v < t2) t2 = v
        }
        if (y < height - 1 && flags[(y + 1) * width + x] == KNOWN) {
             val v = t[(y + 1) * width + x]
             if (v < t2) t2 = v
        }

        if (t1 == MAX_DISTANCE && t2 == MAX_DISTANCE) return MAX_DISTANCE
        if (t1 == MAX_DISTANCE) return 1.0f + t2
        if (t2 == MAX_DISTANCE) return 1.0f + t1
        
        val distSq = (t1 - t2) * (t1 - t2)
        val underRoot = 2.0f - distSq
        if (underRoot < 0f) return MAX_DISTANCE
        
        return (t1 + t2 + sqrt(underRoot)) * 0.5f
    }

    private fun solveInpaint(x: Int, y: Int, width: Int, height: Int, t: FloatArray, flags: IntArray, pixels: IntArray, radius: Int): Int {
        var wSum = 0.0f
        var rSum = 0.0f
        var gSum = 0.0f
        var bSum = 0.0f
        
        // Search neighborhood
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx
                val ny = y + dy
                
                if (nx in 0 until width && ny in 0 until height) {
                    val nOffset = ny * width + nx
                    if (flags[nOffset] == KNOWN) {
                        val dirX = (x - nx).toFloat()
                        val dirY = (y - ny).toFloat()
                        val distSq = dirX * dirX + dirY * dirY
                        
                        if (distSq <= 0.001f) continue

                        val w = 1.0f / (distSq + 0.01f)
                        
                        val pixel = pixels[nOffset]
                        rSum += Color.red(pixel).toFloat() * w
                        gSum += Color.green(pixel).toFloat() * w
                        bSum += Color.blue(pixel).toFloat() * w
                        wSum += w
                    }
                }
            }
        }
        
        if (wSum == 0.0f) return Color.BLACK
        
        return Color.rgb((rSum / wSum).toInt(), (gSum / wSum).toInt(), (bSum / wSum).toInt())
    }
}
