package com.scanner.lab.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.scanner.lab.R

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ScanMode {
        QR, BARCODE, DOCUMENT
    }

    private var scanMode = ScanMode.QR
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintClear = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#80FFFFFF") // Semi-transparent white
    }

    // Stealth Palette Overlays
    // Glass Overlay: #1E293B at 60% opacity
    private val maskColor = Color.parseColor("#991E293B") 
    // Document Mode: Lighter mask (approx 20%) using same base color
    private val documentMaskColor = Color.parseColor("#331E293B") 
    private val scanRect = RectF()

    fun setScanMode(mode: ScanMode) {
        scanMode = mode
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // 1. Draw dimmer mask
        // For DOCUMENT mode, we use a lighter mask or none, but 'usual market' often implies 
        // focus on the grid. We will use a lighter mask for document to keep focus.
        val layerId = canvas.saveLayer(0f, 0f, width, height, null)
        
        if (scanMode == ScanMode.DOCUMENT) {
             canvas.drawColor(documentMaskColor)
        } else {
             canvas.drawColor(maskColor)
        }

        // 2. Calculate Rect
        val cx = width / 2
        val cy = height / 2
        
        val rectW: Float
        val rectH: Float

        when (scanMode) {
            ScanMode.QR -> {
                // Square (approx 70% of width)
                val side = width * 0.7f
                rectW = side
                rectH = side
            }
            ScanMode.BARCODE -> {
                // Barcode (Rectangular: Wider and shorter)
                rectW = width * 0.85f
                rectH = width * 0.4f
            }
            ScanMode.DOCUMENT -> {
                // Document (Large Rectangle: 5/6 width, 3/4 height approx, essentially full screen padding)
                rectW = width * 0.9f
                rectH = height * 0.85f
            }
        }

        val left = cx - rectW / 2
        val top = cy - rectH / 2
        val right = cx + rectW / 2
        val bottom = cy + rectH / 2

        scanRect.set(left, top, right, bottom)

        // 3. Clear the cutout (Cut the hole)
        canvas.drawRoundRect(scanRect, 16f, 16f, paintClear)

        // 4. Draw Corners
        drawCorners(canvas, scanRect)

        // 5. Draw Grid
        if (scanMode == ScanMode.QR) {
            drawGrid(canvas, scanRect, 8)
        } else if (scanMode == ScanMode.DOCUMENT) {
            // Market practice: 3x3 grid (Rule of Thirds)
            drawGrid(canvas, scanRect, 3)
        } else {
            // For Barcode, center line
             drawCenterLine(canvas, scanRect)
        }

        canvas.restoreToCount(layerId)
    }

    private fun drawCorners(canvas: Canvas, rect: RectF) {
        val cornerLength = 60f
        
        // Top Left
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, paintBorder)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLength, paintBorder)

        // Top Right
        canvas.drawLine(rect.right, rect.top, rect.right - cornerLength, rect.top, paintBorder)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, paintBorder)

        // Bottom Left
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, paintBorder)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLength, paintBorder)

        // Bottom Right
        canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLength, rect.bottom, paintBorder)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, paintBorder)
    }

    private fun drawGrid(canvas: Canvas, rect: RectF, divisions: Int) {
        val stepX = rect.width() / divisions
        val stepY = rect.height() / divisions
        
        // Vertical lines
        for (i in 1 until divisions) {
            val x = rect.left + i * stepX
            canvas.drawLine(x, rect.top, x, rect.bottom, paintGrid)
        }
        // Horizontal lines
        for (i in 1 until divisions) {
            val y = rect.top + i * stepY
            canvas.drawLine(rect.left, y, rect.right, y, paintGrid)
        }
    }
    
    private fun drawCenterLine(canvas: Canvas, rect: RectF) {
        val cy = rect.centerY()
        val paintRed = Paint(paintGrid).apply { 
            color = Color.RED 
            strokeWidth = 4f
        }
        canvas.drawLine(rect.left + 20f, cy, rect.right - 20f, cy, paintRed)
    }
}
