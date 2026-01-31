package com.plainlabs.qrpdftools.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.plainlabs.qrpdftools.R

class ScanningOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var laserY = 0f
    private var isScanning = false
    private var animator: ValueAnimator? = null
    
    // Laser properties
    private val laserColor = ContextCompat.getColor(context, R.color.primary_blue) // Fallback or standard
    private val laserHeight = 4f * resources.displayMetrics.density
    
    // Gradient shader for the laser
    private var laserShader: LinearGradient? = null

    init {
        paint.color = laserColor
        paint.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupAnimator(h.toFloat())
        setupShader(w.toFloat())
    }

    private fun setupShader(width: Float) {
        // Transparent -> Color -> Transparent gradient
        laserShader = LinearGradient(
            0f, 0f, width, 0f,
            intArrayOf(Color.TRANSPARENT, laserColor, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun setupAnimator(height: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, height).apply {
            duration = 2000L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                laserY = it.animatedValue as Float
                invalidate()
            }
        }
        if (isScanning) animator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isScanning) return

        paint.shader = laserShader
        
        // Draw Main Laser Line
        canvas.drawRect(0f, laserY, width.toFloat(), laserY + laserHeight, paint)
        
        // Draw "Glow" (Simulated by wider, more transparent rects)
        paint.alpha = 100
        canvas.drawRect(0f, laserY - 4f, width.toFloat(), laserY + laserHeight + 4f, paint)
        
        paint.alpha = 255 // Reset
    }

    fun startScanning() {
        if (isScanning) return
        isScanning = true
        visibility = View.VISIBLE
        animator?.start()
    }

    fun stopScanning() {
        isScanning = false
        visibility = View.GONE
        animator?.cancel()
    }
}
