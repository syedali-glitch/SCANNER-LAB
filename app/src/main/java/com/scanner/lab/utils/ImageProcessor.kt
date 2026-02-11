package com.scanner.lab.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * High-End Image Processing Engine
 * Handles Perspective Correction and Filters
 */
object ImageProcessor {

    /**
     * Apply Perspective Transformation using Native Android Matrix (setPolyToPoly).
     * This avoids OpenCV dependencies while providing decent de-warping.
     * @param original Full captured bitmap
     * @param corners List of 4 points (TL, TR, BR, BL) in bitmap coordinates
     */
    suspend fun correctPerspective(original: Bitmap, corners: List<PointF>): Result<Bitmap> = withContext(Dispatchers.Default) {
        ErrorHandler.safe("PerspectiveCorrection") {
            if (corners.size != 4) return@safe original

            // 1. Calculate dimensions of the new document (Max Width/Height)
            val wTop = distance(corners[0], corners[1])
            val wBottom = distance(corners[3], corners[2])
            val maxWidth = max(wTop, wBottom).toInt()

            val hLeft = distance(corners[0], corners[3])
            val hRight = distance(corners[1], corners[2])
            val maxHeight = max(hLeft, hRight).toInt()

            // 2. Create Destination Points (Rectangular)
            // Order: TL, TR, BR, BL
            val src = floatArrayOf(
                corners[0].x, corners[0].y, // TL
                corners[1].x, corners[1].y, // TR
                corners[2].x, corners[2].y, // BR
                corners[3].x, corners[3].y  // BL
            )

            val dst = floatArrayOf(
                0f, 0f,                 // TL
                maxWidth.toFloat(), 0f, // TR
                maxWidth.toFloat(), maxHeight.toFloat(), // BR
                0f, maxHeight.toFloat() // BL
            )

            // 3. Compute Matrix
            val matrix = android.graphics.Matrix()
            // pointCount=4 for perspective
            val success = matrix.setPolyToPoly(src, 0, dst, 0, 4)

            if (!success) {
                // Fallback: Just return original if transform fails (e.g. collinear points)
                return@safe original
            }

            // 4. Create Tranformed Bitmap
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        }
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    /**
     * Apply "Magic" or "B&W" filters
     */
    fun applyFilter(original: Bitmap, filterMode: FilterMode): Bitmap {
        val width = original.width
        val height = original.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        when (filterMode) {
            FilterMode.B_AND_W -> {
                // High Contrast Grayscale
                colorMatrix.setSaturation(0f)
                val scale = 1.3f // Reduced from 1.5 to preserve some detail
                val translate = (-.5f * scale + .5f) * 255f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(contrastMatrix)
            }
            FilterMode.MAGIC -> {
                // "Magic" Whiteboard enhancer
                colorMatrix.setSaturation(1.1f) // Slight sat boost
                val scale = 1.1f
                val translate = (-.1f * scale + .1f) * 255f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(contrastMatrix)
            }
            FilterMode.MAGIC_V2 -> {
                // Histogram Stretch (Simulated via Matrix for speed) + Saturation
                colorMatrix.setSaturation(1.3f) 
                // Aggressive contrast to pop text
                val scale = 1.3f
                val translate = (-.2f * scale + .2f) * 255f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(contrastMatrix)
                
                // Note: Real Histogram stretching requires pixel access (slow in Java/Kotlin).
                // Use SHADOW_REMOVER for heavy lifting.
            }
            FilterMode.SHADOW_REMOVER -> {
                // Return processed bitmap directly from removeShadows
                return removeShadows(original)
            }
            FilterMode.SECURITY_PATTERN -> {
                // Overlay repeating "SECURE COPY" text
                val filtered = original.copy(Bitmap.Config.ARGB_8888, true)
                val c = Canvas(filtered)
                val p = Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    alpha = 60 // Low opacity
                    textSize = height / 20f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                
                c.save()
                c.rotate(-45f, width / 2f, height / 2f)
                
                val text = "SECURE COPY  "
                val textWidth = p.measureText(text)
                // Draw grid
                for (y in -height until height * 2 step (p.textSize * 3).toInt()) {
                    for (x in -width until width * 2 step (textWidth * 1.5).toInt()) {
                        c.drawText(text, x.toFloat(), y.toFloat(), p)
                    }
                }
                
                c.restore()
                return filtered
            }
            FilterMode.ORIGINAL -> {
                // No-op
            }
        }

        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(original, 0f, 0f, paint)

        return bitmap
    }
    
    /**
     * Remove Shadows using Background Estimation (Divisive Normalization)
     */
    private fun removeShadows(original: Bitmap): Bitmap {
        val scaleFactor = 0.125f
        val w = (original.width * scaleFactor).toInt().coerceAtLeast(10)
        val h = (original.height * scaleFactor).toInt().coerceAtLeast(10)
        
        val small = Bitmap.createScaledBitmap(original, w, h, true)
        
        fastBlur(small, 10) 
        fastBlur(small, 10)
        
        val background = Bitmap.createScaledBitmap(small, original.width, original.height, true)
        small.recycle()
        
        val width = original.width
        val height = original.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        val bgPixels = IntArray(width * height)
        
        original.getPixels(pixels, 0, width, 0, 0, width, height)
        background.getPixels(bgPixels, 0, width, 0, 0, width, height)
        background.recycle()
        
        for (i in pixels.indices) {
            val p = pixels[i]
            val bg = bgPixels[i]
            
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            
            val bgR = ((bg shr 16) and 0xFF).coerceAtLeast(1)
            val bgG = ((bg shr 8) and 0xFF).coerceAtLeast(1)
            val bgB = (bg and 0xFF).coerceAtLeast(1)
            
            val newR = (r * 255 / bgR).coerceAtMost(255)
            val newG = (g * 255 / bgG).coerceAtMost(255)
            val newB = (b * 255 / bgB).coerceAtMost(255)
            
            pixels[i] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
        }
        
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int) {
         val smallW = (sentBitmap.width / 4).coerceAtLeast(1)
         val smallH = (sentBitmap.height / 4).coerceAtLeast(1)
         val tiny = Bitmap.createScaledBitmap(sentBitmap, smallW, smallH, true)
         val blurred = Bitmap.createScaledBitmap(tiny, sentBitmap.width, sentBitmap.height, true)
         
         val canvas = Canvas(sentBitmap)
         canvas.drawBitmap(blurred, 0f, 0f, Paint())
         
         tiny.recycle()
         blurred.recycle()
    }

    enum class FilterMode {
        ORIGINAL,
        B_AND_W,
        MAGIC,
        MAGIC_V2,
        SHADOW_REMOVER,
        SECURITY_PATTERN
    }
}
