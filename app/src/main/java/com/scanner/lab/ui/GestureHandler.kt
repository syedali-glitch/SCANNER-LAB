package com.scanner.lab.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.abs

/**
 * Advanced gesture handler with smooth visual feedback
 */
class GestureHandler(
    private val context: Context,
    private val view: View
) {
    
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        private const val LONG_PRESS_SCALE = 0.95f
        private const val DOUBLE_TAP_SCALE = 0.9f
    }
    
    private var onSwipeLeft: (() -> Unit)? = null
    private var onSwipeRight: (() -> Unit)? = null
    private var onSwipeUp: (() -> Unit)? = null
    private var onSwipeDown: (() -> Unit)? = null
    private var onLongPress: (() -> Unit)? = null
    private var onDoubleTap: (() -> Unit)? = null
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x
            
            if (abs(diffX) > abs(diffY)) {
                // Horizontal swipe
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight?.invoke()
                        animateSwipe(diffX)
                    } else {
                        onSwipeLeft?.invoke()
                        animateSwipe(diffX)
                    }
                    return true
                }
            } else {
                // Vertical swipe
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeDown?.invoke()
                    } else {
                        onSwipeUp?.invoke()
                    }
                    return true
                }
            }
            
            return false
        }
        
        override fun onLongPress(e: MotionEvent) {
            onLongPress?.invoke()
            animateLongPress()
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap?.invoke()
            animateDoubleTap()
            return true
        }
    })
    
    init {
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }
    
    /**
     * Swipe animation
     */
    private fun animateSwipe(diffX: Float) {
        val direction = if (diffX > 0) 20f else -20f
        val translationX = ObjectAnimator.ofFloat(view, "translationX", 0f, direction, 0f)
        
        translationX.apply {
            duration = 200
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }
    
    /**
     * Long press animation
     */
    private fun animateLongPress() {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, LONG_PRESS_SCALE, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, LONG_PRESS_SCALE, 1.0f)
        
        scaleX.apply {
            duration = 200
            start()
        }
        scaleY.apply {
            duration = 200
            start()
        }
    }
    
    /**
     * Double tap animation
     */
    private fun animateDoubleTap() {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, DOUBLE_TAP_SCALE, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, DOUBLE_TAP_SCALE, 1.0f)
        
        scaleX.apply {
            duration = 150
            interpolator = OvershootInterpolator(2.0f)
            start()
        }
        scaleY.apply {
            duration = 150
            interpolator = OvershootInterpolator(2.0f)
            start()
        }
    }
    
    // Setters for callbacks
    fun setOnSwipeLeft(callback: () -> Unit): GestureHandler {
        onSwipeLeft = callback
        return this
    }
    
    fun setOnSwipeRight(callback: () -> Unit): GestureHandler {
        onSwipeRight = callback
        return this
    }
    
    fun setOnSwipeUp(callback: () -> Unit): GestureHandler {
        onSwipeUp = callback
        return this
    }
    
    fun setOnSwipeDown(callback: () -> Unit): GestureHandler {
        onSwipeDown = callback
        return this
    }
    
    fun setOnLongPress(callback: () -> Unit): GestureHandler {
        onLongPress = callback
        return this
    }
    
    fun setOnDoubleTap(callback: () -> Unit): GestureHandler {
        onDoubleTap = callback
        return this
    }
}
