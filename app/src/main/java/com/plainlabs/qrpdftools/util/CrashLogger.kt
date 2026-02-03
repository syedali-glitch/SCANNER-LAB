package com.plainlabs.qrpdftools.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {
    
    private const val TAG = "CrashLogger"
    private const val LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10
    
    private var crashLogFile: File? = null
    
    /**
     * Initialize crash logging system
     */
    fun init(context: Context) {
        try {
            // Create logs directory
            val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }
            
            // Create crash log file
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            crashLogFile = File(logsDir, "crash_$timestamp.log")
            
            // Clean old log files
            cleanOldLogs(logsDir)
            
            // Write initial system info
            logSystemInfo(context)
            
            // Set up global exception handler
            setupGlobalExceptionHandler(context)
            
            log("CrashLogger initialized successfully")
            Log.d(TAG, "Crash logs location: ${crashLogFile?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CrashLogger", e)
        }
    }
    
    /**
     * Set up global uncaught exception handler
     */
    private fun setupGlobalExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrash(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            } finally {
                // Call default handler
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
    
    /**
     * Log system information
     */
    private fun logSystemInfo(context: Context) {
        log("========================================")
        log("SYSTEM INFORMATION")
        log("========================================")
        log("App Version: ${getAppVersion(context)}")
        log("Package: ${context.packageName}")
        log("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        log("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        log("Board: ${Build.BOARD}")
        log("Hardware: ${Build.HARDWARE}")
        log("Product: ${Build.PRODUCT}")
        log("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        log("========================================\n")
    }
    
    /**
     * Log a crash with full details
     */
    private fun logCrash(thread: Thread, throwable: Throwable) {
        log("\n========================================")
        log("CRASH DETECTED!")
        log("========================================")
        log("Thread: ${thread.name}")
        log("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        log("Exception: ${throwable.javaClass.name}")
        log("Message: ${throwable.message}")
        log("\nStack Trace:")
        log(getStackTraceString(throwable))
        log("========================================\n")
    }
    
    /**
     * Log a message with tag
     */
    fun log(tag: String, message: String) {
        val formattedMessage = "[$tag] $message"
        log(formattedMessage)
        Log.d(tag, message)
    }
    
    /**
     * Log an error with exception
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val formattedMessage = "[$tag] ERROR: $message"
        log(formattedMessage)
        
        if (throwable != null) {
            log(getStackTraceString(throwable))
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log a simple message
     */
    private fun log(message: String) {
        try {
            crashLogFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(message)
                    writer.append("\n")
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to crash log", e)
        }
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = java.io.StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Clean old log files, keep only MAX_LOG_FILES newest
     */
    private fun cleanOldLogs(logsDir: File) {
        try {
            val logFiles = logsDir.listFiles()?.filter { it.name.startsWith("crash_") }
                ?.sortedByDescending { it.lastModified() }
            
            logFiles?.drop(MAX_LOG_FILES)?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old logs", e)
        }
    }
    
    /**
     * Get crash log file path for sharing
     */
    fun getCrashLogPath(): String? {
        return crashLogFile?.absolutePath
    }
}
