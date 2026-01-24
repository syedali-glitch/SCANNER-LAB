package com.plainlabs.qrpdftools.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Handle error with logging and user-friendly message
     */
    fun handleError(
        tag: String,
        error: Throwable,
        userMessage: String = "An error occurred",
        isCritical: Boolean = false
    ): String {
        // Log full stack trace
        val stackTrace = getStackTraceString(error)
        Log.e(tag, "$userMessage: ${error.message}")
        Log.e(tag, stackTrace)
        
        if (isCritical) {
            Log.wtf(tag, "CRITICAL ERROR: $userMessage", error)
        }
        
        return userMessage
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * Safe execution with error handling
     */
    inline fun <T> safe(
        tag: String = TAG,
        defaultValue: T,
        userMessage: String = "Operation failed",
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            handleError(tag, e, userMessage)
            defaultValue
        }
    }
    
    /**
     * Safe execution with nullable return
     */
    inline fun <T> safeNullable(
        tag: String = TAG,
        userMessage: String = "Operation failed",
        block: () -> T?
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleError(tag, e, userMessage)
            null
        }
    }
    
    /**
     * Common error messages
     */
    object Messages {
        const val CAMERA_ERROR = "Camera initialization failed"
        const val PERMISSION_ERROR = "Permission denied"
        const val SCAN_ERROR = "Scanning failed"
        const val DATABASE_ERROR = "Database operation failed"
        const val FILE_ERROR = "File operation failed"
        const val PDF_ERROR = "PDF processing failed"
        const val NETWORK_ERROR = "Network operation failed"
        const val AD_ERROR = "Advertisement failed to load"
        const val BILLING_ERROR = "Purchase failed"
        const val UNKNOWN_ERROR = "An unexpected error occurred"
    }
}
