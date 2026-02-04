package com.plainlabs.qrpdftools.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import com.plainlabs.qrpdftools.R

/**
 * GlassUIHelper - Implements version-adaptive glassmorphism as mandated by the Senior Architect.
 * 
 * Performance Strategy:
 * - API 31+: High-performance RenderEffect blur.
 * - API <31: Static semi-transparent fallback (#D9121212) to maintain 60fps.
 */
object GlassUIHelper {

    /**
     * Applies the glassmorphism effect to a view.
     * @param view The view to apply the effect to.
     * @param blurRadius The blur radius for API 31+ (default 20f).
     */
    fun applyGlassEffect(view: View, blurRadius: Float = 20f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ True Blur
            val blurEffect = RenderEffect.createBlurEffect(
                blurRadius, 
                blurRadius, 
                Shader.TileMode.CLAMP
            )
            view.setRenderEffect(blurEffect)
        } else {
            // Legacy Fallback: Static Premium Charcoal with high alpha
            // Defined as @color/glass_fallback in colors.xml
            view.setBackgroundColor(
                ContextCompat.getColor(view.context, R.color.glass_fallback)
            )
        }
    }
    
    /**
     * Removes the glass effect from a view.
     */
    fun removeGlassEffect(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
    }
}
