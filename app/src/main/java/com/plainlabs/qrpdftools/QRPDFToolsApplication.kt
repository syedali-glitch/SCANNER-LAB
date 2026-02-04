package com.plainlabs.qrpdftools

import android.app.Application
import com.plainlabs.qrpdftools.util.CrashLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics

class QRPDFToolsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash logger FIRST - before anything else
        CrashLogger.init(this)
        CrashLogger.log("Application", "App starting...")
        
        // Initialize Firebase Crashlytics for automatic remote crash reporting
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            CrashLogger.log("Application", "Firebase Crashlytics initialized")
        } catch (e: Exception) {
            CrashLogger.logError("Application", "Firebase Crashlytics init failed (missing google-services.json?)", e)
        }
    }
}
