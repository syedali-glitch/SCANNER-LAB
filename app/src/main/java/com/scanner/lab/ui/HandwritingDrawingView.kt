package com.scanner.lab.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.Ink

class HandwritingDrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private var inkBuilder = Ink.builder()
    private var strokeBuilder: Ink.Stroke.Builder? = null

    fun clear() {
        path.reset()
        inkBuilder = Ink.builder()
        invalidate()
    }

    fun getInk(): Ink {
        return inkBuilder.build()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val t = System.currentTimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                strokeBuilder = Ink.Stroke.builder()
                strokeBuilder?.addPoint(Ink.Point.create(x, y, t))
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                strokeBuilder?.addPoint(Ink.Point.create(x, y, t))
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // path.lineTo(x, y) // Optional, move covers it usually
                strokeBuilder?.addPoint(Ink.Point.create(x, y, t))
                strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                strokeBuilder = null
                performClick()
                invalidate()
            }
        }
        return true
    }
    
    override fun performClick(): Boolean {
        return super.performClick()
    }
}
