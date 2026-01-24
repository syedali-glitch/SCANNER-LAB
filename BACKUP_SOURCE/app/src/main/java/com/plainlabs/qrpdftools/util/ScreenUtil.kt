package com.plainlabs.qrpdftools.util

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.pow
import kotlin.math.sqrt

object ScreenUtil {
    
    /**
     * Get screen width in pixels
     */
    fun getScreenWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels
    }
    
    /**
     * Get screen height in pixels
     */
    fun getScreenHeight(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels
    }
    
    /**
     * Get screen density
     */
    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }
    
    /**
     * Get screen DPI category
     */
    fun getDensityBucket(context: Context): String {
        return when (context.resources.displayMetrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> "ldpi (120dpi)"
            DisplayMetrics.DENSITY_MEDIUM -> "mdpi (160dpi)"
            DisplayMetrics.DENSITY_HIGH -> "hdpi (240dpi)"
            DisplayMetrics.DENSITY_XHIGH -> "xhdpi (320dpi)"
            DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi (480dpi)"
            DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi (640dpi)"
            else -> "Unknown (${context.resources.displayMetrics.densityDpi}dpi)"
        }
    }
    
    /**
     * Get screen size in inches (diagonal)
     */
    fun getScreenSizeInches(context: Context): Double {
        val displayMetrics = context.resources.displayMetrics
        val widthInches = displayMetrics.widthPixels / displayMetrics.xdpi
        val heightInches = displayMetrics.heightPixels / displayMetrics.ydpi
        return sqrt(widthInches.pow(2) + heightInches.pow(2).toDouble())
    }
    
    /**
     * Get screen refresh rate
     */
    fun getRefreshRate(activity: Activity): Float {
        return activity.windowManager.defaultDisplay.refreshRate
    }
    
    /**
     * Convert dp to pixels
     */
    fun dpToPx(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    /**
     * Convert pixels to dp
     */
    fun pxToDp(context: Context, px: Float): Int {
        val density = context.resources.displayMetrics.density
        return (px / density).toInt()
    }
    
    /**
     * Get screen size category
     */
    fun getScreenSizeCategory(context: Context): String {
        val screenLayout = context.resources.configuration.screenLayout
        val screenSize = screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        
        return when (screenSize) {
            android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL -> "Small"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL -> "Normal"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE -> "Large"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "XLarge"
            else -> "Unknown"
        }
    }
    
    /**
     * Check if screen is landscape
     */
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == 
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }
    
    /**
     * Get screen resolution string
     */
    fun getResolution(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
    }
    
    /**
     * Get comprehensive screen info
     */
    data class ScreenInfo(
        val widthPx: Int,
        val heightPx: Int,
        val widthDp: Int,
        val heightDp: Int,
        val density: Float,
        val densityDpi: Int,
        val densityBucket: String,
        val sizeInches: Double,
        val refreshRate: Float,
        val sizeCategory: String,
        val isLandscape: Boolean,
        val resolution: String
    )
    
    fun getScreenInfo(activity: Activity): ScreenInfo {
        val context = activity as Context
        val displayMetrics = context.resources.displayMetrics
        
        return ScreenInfo(
            widthPx = displayMetrics.widthPixels,
            heightPx = displayMetrics.heightPixels,
            widthDp = pxToDp(context, displayMetrics.widthPixels.toFloat()),
            heightDp = pxToDp(context, displayMetrics.heightPixels.toFloat()),
            density = displayMetrics.density,
            densityDpi = displayMetrics.densityDpi,
            densityBucket = getDensityBucket(context),
            sizeInches = getScreenSizeInches(context),
            refreshRate = getRefreshRate(activity),
            sizeCategory = getScreenSizeCategory(context),
            isLandscape = isLandscape(context),
            resolution = getResolution(context)
        )
    }
    
    /**
     * Optimize for high refresh rate displays
     */
    fun enableHighRefreshRate(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val modes = display.supportedModes
            
            // Find highest refresh rate mode
            val highestRefreshMode = modes.maxByOrNull { it.refreshRate }
            
            highestRefreshMode?.let { mode ->
                val layoutParams = activity.window.attributes
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    layoutParams.preferredDisplayModeId = mode.modeId
                    activity.window.attributes = layoutParams
                }
            }
        }
    }
}
