package com.plainlabs.qrpdftools.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object DeviceCompatibility {
    
    /**
     * Check if device is a tablet (7-inch or larger)
     */
    fun isTablet(context: Context): Boolean {
        return context.resources.getBoolean(com.plainlabs.qrpdftools.R.bool.isTablet)
    }
    
    /**
     * Check if running on ChromeOS
     */
    fun isChromeOS(context: Context): Boolean {
        return context.packageManager.hasSystemFeature("org.chromium.arc")
    }
    
    /**
     * Check if running on Fire OS (Amazon)
     */
    fun isFireOS(): Boolean {
        return Build.MANUFACTURER.equals("Amazon", ignoreCase = true)
    }
    
    /**
     * Check if device has camera
     */
    fun hasCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    
    /**
     * Check if device has autofocus
     */
    fun hasAutofocus(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)
    }
    
    /**
     * Check if running on Android TV
     */
    fun isTV(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    
    /**
     * Get Android version name
     */
    fun getAndroidVersionName(): String {
        return when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.LOLLIPOP -> "5.0 Lollipop"
            Build.VERSION_CODES.LOLLIPOP_MR1 -> "5.1 Lollipop"
            Build.VERSION_CODES.M -> "6.0 Marshmallow"
            Build.VERSION_CODES.N -> "7.0 Nougat"
            Build.VERSION_CODES.N_MR1 -> "7.1 Nougat"
            Build.VERSION_CODES.O -> "8.0 Oreo"
            Build.VERSION_CODES.O_MR1 -> "8.1 Oreo"
            Build.VERSION_CODES.P -> "9.0 Pie"
            Build.VERSION_CODES.Q -> "10"
            Build.VERSION_CODES.R -> "11"
            Build.VERSION_CODES.S -> "12"
            Build.VERSION_CODES.S_V2 -> "12L"
            Build.VERSION_CODES.TIRAMISU -> "13"
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "14"
            else -> "Unknown (API ${Build.VERSION.SDK_INT})"
        }
    }
    
    /**
     * Check if device supports multi-window
     */
    fun supportsMultiWindow(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    /**
     * Get device form factor description
     */
    fun getDeviceType(context: Context): String {
        return when {
            isTV(context) -> "Android TV"
            isChromeOS(context) -> "ChromeOS"
            isFireOS() -> "Fire OS"
            isTablet(context) -> "Tablet"
            else -> "Phone"
        }
    }
}
