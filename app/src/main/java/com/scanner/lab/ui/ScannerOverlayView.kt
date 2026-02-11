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
        QR, BARCODE, DOCUMENT, ID_CARD, PASSPORT, BOOK
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
    private val paintSpine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.CYAN
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
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
        // For DOCUMENT mode and BOOK mode, we use a lighter mask to keep focus.
        val layerId = canvas.saveLayer(0f, 0f, width, height, null)
        
        if (scanMode == ScanMode.DOCUMENT || scanMode == ScanMode.BOOK) {
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
            ScanMode.DOCUMENT, ScanMode.BOOK -> {
                // Document/Book (Large Rectangle: 5/6 width, 3/4 height approx, essentially full screen padding)
                rectW = width * 0.9f
                rectH = height * 0.85f
            }
            ScanMode.PASSPORT -> {
                // Passport: Wide rectangle, slightly shorter than document (A aspect ratio)
                rectW = width * 0.9f
                rectH = width * 0.9f * 0.7f // Approx passport ratio
            }
            ScanMode.ID_CARD -> {
                rectW = 0f
                rectH = 0f
            }
        }

        val left = cx - rectW / 2
        val top = cy - rectH / 2
        val right = cx + rectW / 2
        val bottom = cy + rectH / 2

        scanRect.set(left, top, right, bottom)

        // Special Case: ID Card (Draws 2 slots, so we handle it separately essentially)
        if (scanMode == ScanMode.ID_CARD) {
            drawIdCardGuides(canvas, width, height)
        } else {
             // 3. Clear the cutout (Cut the hole) for Single Rect
            canvas.drawRoundRect(scanRect, 16f, 16f, paintClear)
            // 4. Draw Corners
            drawCorners(canvas, scanRect)
            // 5. Draw Grid/Guides
            if (scanMode == ScanMode.QR) drawGrid(canvas, scanRect, 8)
            else if (scanMode == ScanMode.DOCUMENT) drawGrid(canvas, scanRect, 3)
            else if (scanMode == ScanMode.PASSPORT) drawPassportGuides(canvas, scanRect)
            else if (scanMode == ScanMode.BOOK) drawBookGuides(canvas, scanRect)
            else drawCenterLine(canvas, scanRect)
        }

        canvas.restoreToCount(layerId)
    }

    private fun drawBookGuides(canvas: Canvas, rect: RectF) {
        // Draw Center Spine
        canvas.drawLine(rect.centerX(), rect.top, rect.centerX(), rect.bottom, paintSpine)
        
        // Draw Curve Guide (Visual Suggestion for Flattening)
        val path = Path()
        val curveDepth = 60f
        
        // Top Curves (Spine to Edges)
        path.moveTo(rect.centerX(), rect.top)
        path.quadTo(rect.left + rect.width() * 0.25f, rect.top + curveDepth, rect.left, rect.top)
        
        path.moveTo(rect.centerX(), rect.top)
        path.quadTo(rect.right - rect.width() * 0.25f, rect.top + curveDepth, rect.right, rect.top)
        
        // Bottom Curves
        path.moveTo(rect.centerX(), rect.bottom)
        path.quadTo(rect.left + rect.width() * 0.25f, rect.bottom - curveDepth, rect.left, rect.bottom)
        
        path.moveTo(rect.centerX(), rect.bottom)
        path.quadTo(rect.right - rect.width() * 0.25f, rect.bottom - curveDepth, rect.right, rect.bottom)
        
        val paintCurve = Paint(paintSpine).apply {
             color = Color.parseColor("#FF4081") // Accent Pink
             style = Paint.Style.STROKE
             strokeWidth = 3f
             pathEffect = null
        }
        canvas.drawPath(path, paintCurve)
        
        // Draw Label
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
             color = Color.CYAN
             textSize = 36f
             textAlign = Paint.Align.CENTER
             setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        canvas.drawText("ALIGN BINDING TO CENTER", rect.centerX(), rect.top - 40f, textPaint)
    }

    private fun drawPassportGuides(canvas: Canvas, rect: RectF) {
        // Draw the MRZ Zone (Bottom part of the rect)
        val mrzHeight = rect.height() * 0.25f
        val mrzRect = RectF(rect.left, rect.bottom - mrzHeight, rect.right, rect.bottom)
        
        val paintMrz = Paint(paintGrid).apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 4f
            pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
        }
        
        canvas.drawRect(mrzRect, paintMrz)
        
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        
        canvas.drawText("ALIGN PASSPORT HERE", rect.centerX(), rect.top - 30f, textPaint)
        canvas.drawText("MRZ ZONE >>>", mrzRect.centerX(), mrzRect.centerY() + 10f, textPaint)
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
    
    private fun drawIdCardGuides(canvas: Canvas, width: Float, height: Float) {
        val cx = width / 2
        val inputAspect = 1.58f // Standard ID Card ratio
        
        // We want the cards to be legible, so let's use a good portion of width (e.g., 70%)
        val cardW = width * 0.7f
        val cardH = cardW / inputAspect
        
        // Vertical spacing
        val gap = 64f
        val totalH = (cardH * 2) + gap
        val startY = (height - totalH) / 2
        
        // Front Card
        val frontRect = RectF(cx - cardW/2, startY, cx + cardW/2, startY + cardH)
        canvas.drawRoundRect(frontRect, 16f, 16f, paintClear)
        drawCorners(canvas, frontRect)
        
        // Back Card
        val backRect = RectF(cx - cardW/2, frontRect.bottom + gap, cx + cardW/2, frontRect.bottom + gap + cardH)
        canvas.drawRoundRect(backRect, 16f, 16f, paintClear)
        drawCorners(canvas, backRect)
        
        // Labels (Text)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        
        canvas.drawText("FRONT", frontRect.centerX(), frontRect.top - 20f, textPaint)
        canvas.drawText("BACK", backRect.centerX(), backRect.top - 20f, textPaint)
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
