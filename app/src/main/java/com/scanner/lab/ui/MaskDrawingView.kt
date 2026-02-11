package com.scanner.lab.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class MaskDrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var path = Path()
    private val paint = Paint().apply {
        color = Color.RED
        alpha = 128 // Semi-transparent
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 50f
        isAntiAlias = true
    }
    
    // Bitmap for the mask extraction
    fun getMaskBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK) // Background is valid
        
        val maskPaint = Paint(paint)
        maskPaint.color = Color.WHITE // Mask is white
        maskPaint.alpha = 255
        maskPaint.style = Paint.Style.STROKE
        maskPaint.strokeWidth = 50f // Match view
        
        canvas.drawPath(path, maskPaint)
        return bitmap
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // path.lineTo(x, y)
                performClick() 
                invalidate()
            }
        }
        return true
    }
    
    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun clear() {
        path.reset()
        invalidate()
    }
}
