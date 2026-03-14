package com.scanner.lab.utils

import android.app.Activity
import android.os.Debug
import androidx.appcompat.app.AlertDialog
import java.io.File
import kotlin.system.exitProcess

object AntiReverseEngineering {

    fun check(activity: Activity) {
        if (isDebugging() || isHooked() || hasSuspiciousFiles() || isRunningOnEmulator()) {
            showFunnyMessageAndExit(activity)
        }
    }

    private fun isDebugging(): Boolean {
        // Checks if a Java debugger is actively attached
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    private fun isHooked(): Boolean {
        // Scans the stack trace for known hooking frameworks
        try {
            throw Exception("HookCheck")
        } catch (e: Exception) {
            for (element in e.stackTrace) {
                val name = element.className.lowercase()
                if (name.contains("xposed") || 
                    name.contains("frida") || 
                    name.contains("substrate") || 
                    name.contains("edxposed") || 
                    name.contains("lsposed")) {
                    return true
                }
            }
        }
        return false
    }

    private fun hasSuspiciousFiles(): Boolean {
        // Checks for common root and hacking binaries
        val paths = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/su",
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (e: Exception) {
                // Ignore file access errors
            }
        }
        return false
    }
    
    private fun isRunningOnEmulator(): Boolean {
        // Simple heuristic to detect emulators often used for reverse engineering
        val buildDetails = (android.os.Build.FINGERPRINT + 
                            android.os.Build.DEVICE + 
                            android.os.Build.MODEL + 
                            android.os.Build.BRAND + 
                            android.os.Build.PRODUCT + 
                            android.os.Build.MANUFACTURER + 
                            android.os.Build.HARDWARE).lowercase()
        return buildDetails.contains("generic") || 
               buildDetails.contains("emulator") || 
               buildDetails.contains("nox") || 
               buildDetails.contains("bluestacks") || 
               buildDetails.contains("genymotion")
    }

    private fun showFunnyMessageAndExit(activity: Activity) {
        val message = "Hold on there, Mr. Hacker! \uD83D\uDD75\uFE0F\u200D\u2642\uFE0F\n\n" +
                "Error 404: Source code not found...\n\n" +
                "Just kidding. But seriously, this app is protected by highly advanced, definitely-nowhere-near-stack-overflow alien technology \uD83D\uDEFC.\n\n" +
                "Put down the decompiler and step away slowly!"

        try {
            AlertDialog.Builder(activity)
                .setTitle("\uD83D\uDEA8 Security Alert!")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("I surrender \uD83D\uDE4C") { _, _ ->
                    exitApp()
                }
                .show()
        } catch (e: Exception) {
            exitApp() // If dialog fails, just quit silently
        }
    }

    private fun exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }
}
