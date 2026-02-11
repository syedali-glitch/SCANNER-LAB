package com.scanner.lab.tools

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.scanner.lab.BaseActivity
import com.scanner.lab.databinding.ActivityAnnotationBinding
import com.scanner.lab.ui.AnnotationView
import com.scanner.lab.utils.ScopedStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnotationActivity : BaseActivity() {

    private lateinit var binding: ActivityAnnotationBinding
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityAnnotationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageUri = intent.data
        if (imageUri == null) {
            Toast.makeText(this, "No image to edit", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadImage()
        setupListeners()
    }

    private fun loadImage() {
        binding.progressLayout.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@AnnotationActivity, imageUri!!, "jpg")
                val bitmap = BitmapFactory.decodeFile(cacheFile?.absolutePath)
                
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        binding.annotationView.setImageBitmap(bitmap)
                    } else {
                        Toast.makeText(this@AnnotationActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    binding.progressLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AnnotationActivity, "Error loading: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnRedPen.setOnClickListener { binding.annotationView.setMode(AnnotationView.AnnotationMode.PEN_RED) }
        binding.btnBluePen.setOnClickListener { binding.annotationView.setMode(AnnotationView.AnnotationMode.PEN_BLUE) }
        binding.btnHighlighter.setOnClickListener { binding.annotationView.setMode(AnnotationView.AnnotationMode.HIGHLIGHTER) }
        binding.btnEraser.setOnClickListener { binding.annotationView.setMode(AnnotationView.AnnotationMode.ERASER) }

        binding.btnSave.setOnClickListener { saveImage() }
    }

    private fun saveImage() {
        binding.progressLayout.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = binding.annotationView.getResultBitmap()
                if (result != null) {
                    val list = listOf(result)
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    val fileName = "Annotated_$timestamp.jpg"
                    
                    val uri = ScopedStorageHelper.createDocumentUri(this@AnnotationActivity, fileName, "image/jpeg")
                    if (uri != null) {
                         contentResolver.openOutputStream(uri)?.use { out ->
                             result.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                         }
                         ScopedStorageHelper.finalizeFile(this@AnnotationActivity, uri)
                         
                         withContext(Dispatchers.Main) {
                             Toast.makeText(this@AnnotationActivity, "Saved to Documents!", Toast.LENGTH_LONG).show()
                             finish()
                         }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AnnotationActivity, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressLayout.visibility = View.GONE
                }
            }
        }
    }
}
