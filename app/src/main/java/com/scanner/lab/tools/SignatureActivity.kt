package com.scanner.lab.tools

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.scanner.lab.BaseActivity
import com.scanner.lab.databinding.ActivitySignatureBinding
import com.scanner.lab.utils.ScopedStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SignatureActivity : BaseActivity() {

    private lateinit var binding: ActivitySignatureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignatureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnClear.setOnClickListener {
            binding.signatureView.clear()
        }
        
        binding.btnSave.setOnClickListener {
            saveSignature()
        }
    }

    private fun saveSignature() {
        if (binding.signatureView.isEmpty()) {
            Toast.makeText(this, "Please draw a signature first", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = binding.signatureView.getSignatureBitmap()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "Signature_$timestamp.png"
                
                // Save as PNG with transparency
                val uri = ScopedStorageHelper.createDocumentUri(this@SignatureActivity, fileName, "image/png")
                
                if (uri != null) {
                     contentResolver.openOutputStream(uri)?.use { out ->
                         bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                     }
                     ScopedStorageHelper.finalizeFile(this@SignatureActivity, uri)
                     
                     withContext(Dispatchers.Main) {
                         Toast.makeText(this@SignatureActivity, "Saved to Documents!", Toast.LENGTH_LONG).show()
                         finish()
                     }
                } else {
                    throw Exception("Failed to create URI")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignatureActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * Custom View for Drawing Signature
 */
class SignatureView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var isEmpty = true

    fun clear() {
        path.reset()
        isEmpty = true
        invalidate()
    }

    fun isEmpty(): Boolean = isEmpty

    fun getSignatureBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT) // Transparent background
        canvas.drawPath(path, paint)
        return bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                lastX = x
                lastY = y
                isEmpty = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(x - lastX)
                val dy = Math.abs(y - lastY)
                if (dx >= 4 || dy >= 4) {
                    path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                    lastX = x
                    lastY = y
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(x, y)
                invalidate()
            }
        }
        return true
    }
}
