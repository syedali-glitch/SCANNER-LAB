package com.scanner.lab

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scanner.lab.converters.ExcelGenerator
import com.scanner.lab.databinding.ActivityConverterBinding
import com.scanner.lab.utils.ScopedStorageHelper
import com.scanner.lab.utils.UserPremiums
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding
    
    // ML Kit
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Image Picker
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            processImagesToExcel(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.cardImgToExcel.setOnClickListener {
            // Check Pro
            if (UserPremiums.isPro) { // defaults to true now
                imagePicker.launch("image/*")
            } else {
                Toast.makeText(this, "Pro Feature Only", Toast.LENGTH_SHORT).show()
                // Or simulate upsell
            }
        }
    }
    
    private fun processImagesToExcel(uris: List<Uri>) {
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val extractedTexts = mutableListOf<String>()
                
                // 1. Process each image with OCR
                uris.forEach { uri ->
                    try {
                        val inputImage = InputImage.fromFilePath(this@ConverterActivity, uri)
                        // Use Tasks.await directly since we lack coroutines-play-services dependency
                        val result = com.google.android.gms.tasks.Tasks.await(recognizer.process(inputImage))
                        extractedTexts.add(result.text)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        extractedTexts.add("[Error processing image]")
                    }
                }
                
                // 2. Generate Excel
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "ScanData_$timestamp.xlsx"
                
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ) ?: throw Exception("Could not create Excel file")
                
                contentResolver.openOutputStream(outputUri)?.use { out ->
                    ExcelGenerator.generateExcel(this@ConverterActivity, extractedTexts, out)
                }
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ConverterActivity, "Excel Saved to Documents!", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ConverterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
}
