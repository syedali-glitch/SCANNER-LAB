package com.plainlabs.qrpdftools

import android.app.Application
import com.plainlabs.qrpdftools.util.CrashLogger

class QRPDFToolsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash logger FIRST - before anything else
        CrashLogger.init(this)
        CrashLogger.log("Application", "App starting...")
    }
}
