package com.plainlabs.qrpdftools.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create UI programmatically - no XML, no resources, no dependencies
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val textView = TextView(this).apply {
            text = "✅ APP WORKS!\n\n" +
                   "Version: Android ${android.os.Build.VERSION.SDK_INT}\n" +
                   "Device: ${android.os.Build.MODEL}\n\n" +
                   "If you see this, the app launches successfully.\n\n" +
                   "Problem was likely:\n" +
                   "- Layout XML resources\n" +
                   "- ViewBinding\n" +
                   "- Navigation Component\n" +
                   "- Theme compatibility"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
        }
        
        layout.addView(textView)
        layout.setBackgroundColor(android.graphics.Color.WHITE)
        setContentView(layout)
    }
}
