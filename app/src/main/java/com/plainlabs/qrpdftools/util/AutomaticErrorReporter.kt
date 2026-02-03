package com.plainlabs.qrpdftools.util

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

object AutomaticErrorReporter {
    
    // Webhook URL for posting crash reports private const val WEBHOOK_URL = "https://webhook.site/unique-url-here" // TODO: Replace with actual webhook
    
    /**
     * Report crash automatically to remote server
     */
    fun reportCrash(context: Context, thread: Thread, throwable: Throwable) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val crashData = buildCrashReport(context, thread, throwable)
                postToWebhook(crashData)
            } catch (e: Exception) {
                // Silent fail - don't crash while reporting a crash
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Report error automatically
     */
    fun reportError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val errorData = buildErrorReport(context, tag, message, throwable)
                postToWebhook(errorData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Build crash report JSON
     */
    private fun buildCrashReport(context: Context, thread: Thread, throwable: Throwable): JSONObject {
        return JSONObject().apply {
            put("type", "CRASH")
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("app_version", getAppVersion(context))
            put("package_name", context.packageName)
            
            // Device info
            put("android_version", Build.VERSION.RELEASE)
            put("api_level", Build.VERSION.SDK_INT)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("board", Build.BOARD)
            
            // Crash info
            put("thread_name", thread.name)
            put("exception_class", throwable.javaClass.name)
            put("exception_message", throwable.message ?: "No message")
            put("stack_trace", getStackTraceString(throwable))
        }
    }
    
    /**
     * Build error report JSON
     */
    private fun buildErrorReport(context: Context, tag: String, message: String, throwable: Throwable?): JSONObject {
        return JSONObject().apply {
            put("type", "ERROR")
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("tag", tag)
            put("message", message)
            put("android_version", Build.VERSION.RELEASE)
            put("model", Build.MODEL)
            
            if (throwable != null) {
                put("exception", throwable.javaClass.name)
                put("stack_trace", getStackTraceString(throwable))
            }
        }
    }
    
    /**
     * Post crash data to webhook
     */
    private fun postToWebhook(data: JSONObject) {
        try {
            val url = URL(WEBHOOK_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "QRPDFTools-CrashReporter")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(data.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
        } catch (e: Exception) {
            // Silent fail
            e.printStackTrace()
        }
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
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
}
