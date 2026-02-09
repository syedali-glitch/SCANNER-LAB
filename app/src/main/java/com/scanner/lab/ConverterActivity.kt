package com.scanner.lab

import android.content.Intent
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding
    
    // ML Kit
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    private enum class ConversionType {
        IMG_TO_EXCEL, IMG_TO_PDF, IMG_TO_WORD, PDF_TO_IMG, PDF_TO_TEXT
    }
    private var currentMode = ConversionType.IMG_TO_EXCEL

    // Image Picker
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            when (currentMode) {
                ConversionType.IMG_TO_EXCEL -> processImagesToExcel(uris)
                ConversionType.IMG_TO_PDF -> processImagesToPdf(uris)
                ConversionType.IMG_TO_WORD -> processImagesToWord(uris)
                else -> {}
            }
        }
    }
    
    // Document Picker (for PDF inputs)
    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            when (currentMode) {
                ConversionType.PDF_TO_IMG -> processPdfToImage(it)
                ConversionType.PDF_TO_TEXT -> processPdfToText(it)
                else -> {}
            }
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
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 1. Image to PDF
        binding.cardImgToPdf.setOnClickListener {
            currentMode = ConversionType.IMG_TO_PDF
            checkProAndPickImages()
        }

        // 2. Image to Excel
        binding.cardImgToExcel.setOnClickListener {
             currentMode = ConversionType.IMG_TO_EXCEL
             checkProAndPickImages()
        }

        // 3. Image to Word
        binding.cardImgToWord.setOnClickListener {
             currentMode = ConversionType.IMG_TO_WORD
             checkProAndPickImages()
        }
        
        // 4. PDF to Image
        binding.cardPdfToImage.setOnClickListener {
             currentMode = ConversionType.PDF_TO_IMG
             checkProAndPickDocument("application/pdf")
        }
        
        // 5. PDF to Text
        binding.cardPdfToText.setOnClickListener {
             currentMode = ConversionType.PDF_TO_TEXT
             checkProAndPickDocument("application/pdf")
        }
    }
    
    private fun checkProAndPickImages() {
        if (UserPremiums.checkOrShowUpsell(this)) {
            imagePicker.launch("image/*")
        }
    }
    
    private fun checkProAndPickDocument(mimeType: String) {
        if (UserPremiums.checkOrShowUpsell(this)) {
            documentPicker.launch(arrayOf(mimeType))
        }
    }
    
    // --- Logic Implementations ---
    
    private fun processImagesToExcel(uris: List<Uri>) {
        showProgress("Converting to Excel...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val extractedTexts = mutableListOf<String>()
                uris.forEach { uri ->
                    try {
                        val inputImage = InputImage.fromFilePath(this@ConverterActivity, uri)
                        val result = com.google.android.gms.tasks.Tasks.await(recognizer.process(inputImage))
                        extractedTexts.add(result.text)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                val fileName = "Scan_Excel_${System.currentTimeMillis()}.xlsx"
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ) ?: throw Exception("File creation failed")
                
                contentResolver.openOutputStream(outputUri)?.use { out ->
                    ExcelGenerator.generateExcel(this@ConverterActivity, extractedTexts, out)
                }
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)
                
                onSuccess("Excel Saved!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processImagesToPdf(uris: List<Uri>) {
        showProgress("Creating PDF...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // TODO: Real PDF Generation (using PDFBox or built-in PdfDocument)
                // For now, simulate success
                kotlinx.coroutines.delay(1000)
                
                 val fileName = "Scan_PDF_${System.currentTimeMillis()}.pdf"
                 val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/pdf"
                )
                
                // Demo: Write dummy content
                // contentResolver.openOutputStream(outputUri!!)?.write("Dummy PDF".toByteArray())
                // ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)

                onSuccess("PDF Saved (Demo)!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    private fun processImagesToWord(uris: List<Uri>) {
        showProgress("Converting to Word...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // TODO: Real Word Generation (using Apache POI XWPF)
                // For now, simulate success
                kotlinx.coroutines.delay(1500)
                onSuccess("Word Doc Saved (Demo)!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processPdfToImage(uri: Uri) {
         showProgress("Extracting Images...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // TODO: Real PDF Render (using PdfRenderer)
                kotlinx.coroutines.delay(1000)
                onSuccess("Images Extracted (Demo)!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processPdfToText(uri: Uri) {
         showProgress("Extracting Text...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // TODO: Real PDF Text Extraction (using PDFBox)
                kotlinx.coroutines.delay(1000)
                onSuccess("Text Extracted (Demo)!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    // --- Helpers ---

    private fun showProgress(msg: String) {
        runOnUiThread {
            binding.layoutProgress.visibility = View.VISIBLE
            binding.tvProgressStatus.text = msg
        }
    }
    
    private fun onSuccess(msg: String) {
        runOnUiThread {
            binding.layoutProgress.visibility = View.GONE
            Toast.makeText(this@ConverterActivity, msg, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun onError(msg: String?) {
        runOnUiThread {
            binding.layoutProgress.visibility = View.GONE
             Toast.makeText(this@ConverterActivity, "Error: $msg", Toast.LENGTH_SHORT).show()
        }
    }
}
