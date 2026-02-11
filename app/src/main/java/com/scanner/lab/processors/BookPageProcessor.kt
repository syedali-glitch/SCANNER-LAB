package com.scanner.lab.processors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BookPageProcessor {

    /**
     * Splits a book spread (2 pages) into 2 separate bitmaps (Left, Right).
     * Assumes the image is landscape or the user has aligned the book spine to the center.
     */
    suspend fun splitPages(original: Bitmap): Pair<Bitmap, Bitmap> = withContext(Dispatchers.Default) {
        val width = original.width
        val height = original.height
        val midPoint = width / 2
        
        // Configuration: Gutter processing? 
        // For v1.4, we do a clean cut with slight overlap to ensure no text loss in binding.
        val overlap = (width * 0.02f).toInt() // 2% overlap
        
        // Left Page
        val leftWidth = midPoint + overlap
        // Validate bounds
        val safeLeftW = leftWidth.coerceAtMost(width)
        val leftPage = Bitmap.createBitmap(original, 0, 0, safeLeftW, height)
        
        // Right Page
        val rightStart = (midPoint - overlap).coerceAtLeast(0)
        val rightWidth = width - rightStart
        val rightPage = Bitmap.createBitmap(original, rightStart, 0, rightWidth, height)
        
        // Optional: Deskew each page individually?
        // That requires corner detection on each half.
        // For v1.4, we just return the raw split.
        
        Pair(leftPage, rightPage)
    }
}
