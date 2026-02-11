package com.scanner.lab.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class AnnotationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentBitmap: Bitmap? = null
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null

    private var currentPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val path = Path()

    fun setImageBitmap(bitmap: Bitmap) {
        currentBitmap = bitmap
        // Create an overlay bitmap of same size for drawing
        overlayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        overlayCanvas = Canvas(overlayBitmap!!)
        invalidate()
    }

    fun setMode(mode: AnnotationMode) {
        when (mode) {
            AnnotationMode.PEN_RED -> {
                currentPaint.color = Color.RED
                currentPaint.strokeWidth = 8f
                currentPaint.alpha = 255
                currentPaint.xfermode = null
            }
            AnnotationMode.PEN_BLUE -> {
                currentPaint.color = Color.BLUE
                currentPaint.strokeWidth = 8f
                currentPaint.alpha = 255
                currentPaint.xfermode = null
            }
            AnnotationMode.HIGHLIGHTER -> {
                currentPaint.color = Color.YELLOW
                currentPaint.strokeWidth = 40f
                currentPaint.alpha = 100 // Semi-transparent
                currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            AnnotationMode.ERASER -> {
                currentPaint.color = Color.TRANSPARENT
                currentPaint.strokeWidth = 50f
                currentPaint.alpha = 0
                currentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the base image scaled to view
        if (currentBitmap != null) {
            val scale = width.toFloat() / currentBitmap!!.width
            canvas.save()
            canvas.scale(scale, scale)
            
            canvas.drawBitmap(currentBitmap!!, 0f, 0f, null)
            if (overlayBitmap != null) {
                 canvas.drawBitmap(overlayBitmap!!, 0f, 0f, null)
            }
            
            // Draw current path
            canvas.drawPath(path, currentPaint)
            
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentBitmap == null) return false
        
        // Map view coords to bitmap coords
        val scale = width.toFloat() / currentBitmap!!.width
        val x = event.x / scale
        val y = event.y / scale

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                overlayCanvas?.drawPath(path, currentPaint)
                path.reset()
                invalidate()
            }
        }
        return true
    }

    fun getResultBitmap(): Bitmap? {
        if (currentBitmap == null || overlayBitmap == null) return null
        
        // Merge Base + Overlay
        val result = currentBitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        canvas.drawBitmap(overlayBitmap!!, 0f, 0f, null)
        return result
    }

    enum class AnnotationMode {
        PEN_RED, PEN_BLUE, HIGHLIGHTER, ERASER
    }
}
