package com.scanner.lab.processors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * Handles the logic for ID Card Mode.
 * Stitches two bitmaps (Front/Back) onto a single A4-sized canvas.
 */
object IdCardProcessor {

    // A4 Size at 72 DPI is approx 595x842. Let's use a higher res (e.g., 300 DPI approx)
    // 2480 x 3508 is standard A4 at 300 DPI.
    // Let's use a manageable size for mobile memory, e.g., 1240 x 1754 (150 DPI)
    private const val CANVAS_WIDTH = 1240
    private const val CANVAS_HEIGHT = 1754

    fun stitchImages(front: Bitmap, back: Bitmap): Bitmap {
        // 1. Create Output Bitmap (A4)
        val result = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 2. Draw White Background
        canvas.drawColor(Color.WHITE)
        
        // 3. Define Layout
        // We want the cards to be centered horizontally.
        // Front on top half, Back on bottom half.
        // ID Card ratio is ~1.58.
        
        val targetWidth = (CANVAS_WIDTH * 0.6f).toInt() // 60% of page width
        val targetHeight = (targetWidth / 1.58f).toInt()
        
        val centerX = CANVAS_WIDTH / 2
        
        // Front Position (Top third approx)
        val frontTop = CANVAS_HEIGHT * 0.2f
        val frontRect = Rect(
            centerX - targetWidth / 2,
            frontTop.toInt(),
            centerX + targetWidth / 2,
            (frontTop + targetHeight).toInt()
        )
        
        // Back Position (Bottom third approx)
        val backTop = CANVAS_HEIGHT * 0.6f
        val backRect = Rect(
            centerX - targetWidth / 2,
            backTop.toInt(),
            centerX + targetWidth / 2,
            (backTop + targetHeight).toInt()
        )
        
        // 4. Draw Bitmaps
        // Use a paint for filtering
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(front, null, frontRect, paint)
        canvas.drawBitmap(back, null, backRect, paint)
        
        // 5. Draw Labels (Optional, mimics professional copiers)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("FRONT", centerX.toFloat(), frontRect.top - 40f, textPaint)
        canvas.drawText("BACK", centerX.toFloat(), backRect.top - 40f, textPaint)
        
        return result
    }
}
