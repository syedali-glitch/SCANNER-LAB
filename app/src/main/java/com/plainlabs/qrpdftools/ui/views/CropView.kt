package com.plainlabs.qrpdftools.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

/**
 * CropView - Interactive 4-point perspective selector.
 * 
 * Mandate: Native implementation using standard Android View and Paint.
 */
class CropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 0, 255, 255)
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val points = mutableListOf<PointF>()
    private var selectedPoint: Int = -1
    private val touchThreshold = 50f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (points.isEmpty()) {
            // Initial default positions (centered rectangle)
            val padding = 100f
            points.add(PointF(padding, padding))
            points.add(PointF(w - padding, padding))
            points.add(PointF(w - padding, h - padding))
            points.add(PointF(padding, h - padding))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 4) return

        // Draw lines
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        path.lineTo(points[1].x, points[1].y)
        path.lineTo(points[2].x, points[2].y)
        path.lineTo(points[3].x, points[3].y)
        path.close()
        canvas.drawPath(path, linePaint)
        canvas.drawPath(path, paint)

        // Draw points
        for (point in points) {
            canvas.drawCircle(point.x, point.y, 25f, pointPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedPoint = -1
                var minDistance = Float.MAX_VALUE
                for (i in points.indices) {
                    val dist = calculateDistance(x, y, points[i].x, points[i].y)
                    if (dist < touchThreshold && dist < minDistance) {
                        minDistance = dist
                        selectedPoint = i
                    }
                }
                return selectedPoint != -1
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedPoint != -1) {
                    points[selectedPoint].x = x.coerceIn(0f, width.toFloat())
                    points[selectedPoint].y = y.coerceIn(0f, height.toFloat())
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedPoint = -1
            }
        }
        return false
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)))
    }

    fun getCropPoints(): List<PointF> {
        return points.toList()
    }

    fun setInitialPoints(containerWidth: Int, containerHeight: Int, imageWidth: Int, imageHeight: Int) {
        // Logically map image coordinates to view coordinates if needed
        // For now, relies on view size
        invalidate()
    }
}
