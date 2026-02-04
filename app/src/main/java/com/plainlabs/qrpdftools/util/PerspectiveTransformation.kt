package com.plainlabs.qrpdftools.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF

/**
 * PerspectiveTransformation - High-End Scanner Processing Engine.
 * 
 * Uses Native Android Matrix transformations to correct document perspective.
 * Mandate: Efficient, proprietary implementation.
 */
object PerspectiveTransformation {

    /**
     * Warps a bitmap given four source corner points.
     * The result is a rectified (rectangular) document.
     */
    fun transform(bitmap: Bitmap, points: List<PointF>): Bitmap {
        // Sort points: top-left, top-right, bottom-right, bottom-left
        val sortedPoints = sortPoints(points)
        
        val tl = sortedPoints[0]
        val tr = sortedPoints[1]
        val br = sortedPoints[2]
        val bl = sortedPoints[3]
        
        // Calculate dimensions of the output rectangular document
        val widthA = Math.sqrt(Math.pow((br.x - bl.x).toDouble(), 2.0) + Math.pow((br.y - bl.y).toDouble(), 2.0))
        val widthB = Math.sqrt(Math.pow((tr.x - tl.x).toDouble(), 2.0) + Math.pow((tr.y - tl.y).toDouble(), 2.0))
        val maxWidth = Math.max(widthA, widthB).toInt()

        val heightA = Math.sqrt(Math.pow((tr.x - br.x).toDouble(), 2.0) + Math.pow((tr.y - br.y).toDouble(), 2.0))
        val heightB = Math.sqrt(Math.pow((tl.x - bl.x).toDouble(), 2.0) + Math.pow((tl.y - bl.y).toDouble(), 2.0))
        val maxHeight = Math.max(heightA, heightB).toInt()

        val result = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        
        val src = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )
        
        val dst = floatArrayOf(
            0f, 0f,
            maxWidth.toFloat(), 0f,
            maxWidth.toFloat(), maxHeight.toFloat(),
            0f, maxHeight.toFloat()
        )
        
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, matrix, null)
        
        return result
    }

    /**
     * Orders points: Top-Left, Top-Right, Bottom-Right, Bottom-Left.
     */
    private fun sortPoints(points: List<PointF>): List<PointF> {
        val sortedByY = points.sortedBy { it.y }
        val top = sortedByY.take(2).sortedBy { it.x }
        val bottom = sortedByY.takeLast(2).sortedByDescending { it.x }
        return top + bottom
    }
}
