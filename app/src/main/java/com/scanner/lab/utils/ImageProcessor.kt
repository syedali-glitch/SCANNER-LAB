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
                // High Contrast Grayscale (Thresholding-like)
                colorMatrix.setSaturation(0f)
                // Boost contrast
                val scale = 1.5f
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
                // "Magic" Whiteboard enhancer: Boost saturation, slight contrast, whiten background
                // 1. Slight Saturation Boost
                colorMatrix.setSaturation(1.2f)
                
                // 2. Brightness/Contrast
                val scale = 1.2f
                val translate = (-.1f * scale + .1f) * 255f
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(contrastMatrix)
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

    enum class FilterMode {
        ORIGINAL,
        B_AND_W,
        MAGIC
    }
}
