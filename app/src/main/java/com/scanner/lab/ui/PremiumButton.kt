package com.scanner.lab.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatButton

/**
 * Premium button with iOS-exceeding animations
 */
class PremiumButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {
    
    companion object {
        private const val ANIMATION_DURATION = 150L
        private const val SCALE_PRESSED = 0.96f
        private const val SCALE_NORMAL = 1.0f
        private const val OVERSHOOT_TENSION = 1.5f
    }
    
    init {
        setupClickAnimation()
    }
    
    /**
     * Setup automatic click animation
     */
    private fun setupClickAnimation() {
        setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animatePress()
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    animateRelease()
                }
            }
            false // Let onClick handle the actual click
        }
    }
    
    /**
     * Press animation - scale down smoothly
     */
    private fun animatePress() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", SCALE_PRESSED)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", SCALE_PRESSED)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Release animation - bounce back with overshoot
     */
    private fun animateRelease() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", SCALE_NORMAL)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", SCALE_NORMAL)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = ANIMATION_DURATION
            interpolator = OvershootInterpolator(OVERSHOOT_TENSION)
            start()
        }
    }
    
    /**
     * Success animation with bounce
     */
    fun animateSuccess() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", SCALE_NORMAL, 1.1f, SCALE_NORMAL)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", SCALE_NORMAL, 1.1f, SCALE_NORMAL)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            interpolator = OvershootInterpolator(OVERSHOOT_TENSION)
            start()
        }
    }
    
    /**
     * Pulse animation for emphasis
     */
    fun animatePulse() {
        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", SCALE_NORMAL, 1.05f, SCALE_NORMAL)
        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", SCALE_NORMAL, 1.05f, SCALE_NORMAL)
        val alpha = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.8f, 1.0f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Error shake animation
     */
    fun animateError() {
        val translationX = ObjectAnimator.ofFloat(this, "translationX", 0f, -10f, 10f, -5f, 5f, 0f)
        
        translationX.apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
