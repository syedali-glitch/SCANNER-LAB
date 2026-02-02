package com.plainlabs.qrpdftools.ui

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.plainlabs.qrpdftools.R

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "========================================")
        Log.d("MainActivity", "MINIMAL MODE - onCreate STARTED")
        Log.d("MainActivity", "========================================")
        
        try {
            // Create simple TextView instead of complex layout
            val textView = TextView(this)
            textView.text = "App Launched Successfully!\n\n" +
                    "If you see this, the app is working.\n\n" +
                    "This is a minimal test mode.\n" +
                    "Version: ${android.os.Build.VERSION.SDK_INT}"
            textView.textSize = 18f
            textView.setPadding(50, 50, 50, 50)
            
            setContentView(textView)
            
            Log.d("MainActivity", "Minimal UI set successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "CRITICAL CRASH", e)
            e.printStackTrace()
            
            // Last resort - show blank activity with error
            try {
                val errorView = TextView(this)
                errorView.text = "ERROR: ${e.message}\n\n${e.stackTraceToString()}"
                errorView.textSize = 12f
                errorView.setPadding(20, 20, 20, 20)
                setContentView(errorView)
            } catch (e2: Exception) {
                Log.e("MainActivity", "Even error display failed", e2)
            }
        }
    }
}
