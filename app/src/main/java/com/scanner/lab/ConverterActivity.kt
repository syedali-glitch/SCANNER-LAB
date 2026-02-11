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

class ConverterActivity : BaseActivity() {

    private lateinit var binding: ActivityConverterBinding
    
    // ML Kit
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    companion object {
        const val EXTRA_CONVERSION_MODE = "extra_conversion_mode"
        const val MODE_IMG_TO_EXCEL = "img_to_excel"
        const val MODE_IMG_TO_PDF = "img_to_pdf"
        const val MODE_IMG_TO_WORD = "img_to_word"
        const val MODE_IMG_TO_PPT = "img_to_ppt"
        const val MODE_PDF_TO_IMG = "pdf_to_img"
        const val MODE_PDF_TO_TEXT = "pdf_to_text"
        const val MODE_PDF_TO_WORD = "pdf_to_word"
        const val MODE_PDF_TO_PPT = "pdf_to_ppt"
    }

    private enum class ConversionType {
        IMG_TO_EXCEL, IMG_TO_PDF, IMG_TO_WORD, IMG_TO_PPT, PDF_TO_IMG, PDF_TO_TEXT, PDF_TO_WORD, PDF_TO_PPT
    }
    private var currentMode = ConversionType.IMG_TO_EXCEL

    // Image Picker
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            when (currentMode) {
                ConversionType.IMG_TO_EXCEL -> processImagesToExcel(uris)
                ConversionType.IMG_TO_PDF -> processImagesToPdf(uris)
                ConversionType.IMG_TO_WORD -> processImagesToWord(uris)
                ConversionType.IMG_TO_PPT -> processImagesToPpt(uris)
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
                ConversionType.PDF_TO_WORD -> processPdfToWord(it)
                ConversionType.PDF_TO_PPT -> processPdfToPpt(it)
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
        handleIntent()
    }

    private fun handleIntent() {
        val mode = intent.getStringExtra(EXTRA_CONVERSION_MODE) ?: return
        
        binding.root.post {
            when (mode) {
                MODE_IMG_TO_EXCEL -> { currentMode = ConversionType.IMG_TO_EXCEL; checkProAndPickImages() }
                MODE_IMG_TO_PDF -> { currentMode = ConversionType.IMG_TO_PDF; checkProAndPickImages() }
                MODE_IMG_TO_WORD -> { currentMode = ConversionType.IMG_TO_WORD; checkProAndPickImages() }
                MODE_IMG_TO_PPT -> { currentMode = ConversionType.IMG_TO_PPT; checkProAndPickImages() }
                MODE_PDF_TO_IMG -> { currentMode = ConversionType.PDF_TO_IMG; checkProAndPickDocument("application/pdf") }
                MODE_PDF_TO_TEXT -> { currentMode = ConversionType.PDF_TO_TEXT; checkProAndPickDocument("application/pdf") }
                MODE_PDF_TO_WORD -> { currentMode = ConversionType.PDF_TO_WORD; checkProAndPickDocument("application/pdf") }
                MODE_PDF_TO_PPT -> { currentMode = ConversionType.PDF_TO_PPT; checkProAndPickDocument("application/pdf") }
            }
        }
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
        
        // 4. Image to PPT
        binding.cardImgToPpt.setOnClickListener {
             currentMode = ConversionType.IMG_TO_PPT
             checkProAndPickImages()
        }
        
        // 5. PDF to Image
        binding.cardPdfToImage.setOnClickListener {
             currentMode = ConversionType.PDF_TO_IMG
             checkProAndPickDocument("application/pdf")
        }
        
        // 6. PDF to Text
        binding.cardPdfToText.setOnClickListener {
             currentMode = ConversionType.PDF_TO_TEXT
             checkProAndPickDocument("application/pdf")
        }
        
        // 7. PDF to Word
        binding.cardPdfToWord.setOnClickListener {
             currentMode = ConversionType.PDF_TO_WORD
             checkProAndPickDocument("application/pdf")
        }
        
        // 8. PDF to PPT
        binding.cardPdfToPpt.setOnClickListener {
             currentMode = ConversionType.PDF_TO_PPT
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
                val extractedTexts = extractTextFromImages(uris)
                
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
    
    // Extracted helper to reuse OCR logic
    private suspend fun extractTextFromImages(uris: List<Uri>): List<String> {
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
        return extractedTexts
    }
    
    private fun processImagesToPdf(uris: List<Uri>) {
        showProgress("Creating PDF...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "Scan_PDF_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/pdf"
                ) ?: throw Exception("File creation failed")

                // Use NativePdfGenerator
                val result = com.scanner.lab.converters.NativePdfGenerator.generatePdf(
                    this@ConverterActivity,
                    uris,
                    outputUri
                )
                
                result.getOrThrow()
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)

                onSuccess("PDF Saved Successfully!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    private fun processImagesToWord(uris: List<Uri>) {
        showProgress("Converting to Word...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // Prepare images as files
                val imagePaths = mutableListOf<String>()
                uris.forEach { uri ->
                    ScopedStorageHelper.copyUriToCache(this@ConverterActivity, uri, "jpg")?.let {
                        imagePaths.add(it.absolutePath)
                    }
                }

                if (imagePaths.isEmpty()) throw Exception("No valid images found")
                
                // 2. Create File
                val fileName = "Scan_Word_${System.currentTimeMillis()}.docx"
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ) ?: throw Exception("File creation failed")

                // 3. Write DOCX
                val tempFile = ScopedStorageHelper.createCacheFile(this@ConverterActivity, "docx")
                com.scanner.lab.converters.DocxConverter.imagesToDocx(imagePaths, tempFile.absolutePath).getOrThrow()

                // Copy temp to uri
                contentResolver.openOutputStream(outputUri)?.use { out ->
                    java.io.FileInputStream(tempFile).copyTo(out)
                }
                
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)
                onSuccess("Word Doc Saved!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processImagesToPpt(uris: List<Uri>) {
        showProgress("Converting to PowerPoint...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // Prepare images as files
                val imagePaths = mutableListOf<String>()
                uris.forEach { uri ->
                    ScopedStorageHelper.copyUriToCache(this@ConverterActivity, uri, "jpg")?.let {
                        imagePaths.add(it.absolutePath)
                    }
                }

                if (imagePaths.isEmpty()) throw Exception("No valid images found")
                
                // 2. Create File
                val fileName = "Scan_Presentation_${System.currentTimeMillis()}.pptx"
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                ) ?: throw Exception("File creation failed")

                // 3. Write PPTX
                val tempFile = ScopedStorageHelper.createCacheFile(this@ConverterActivity, "pptx")
                com.scanner.lab.converters.PptxConverter.imagesToPptx(imagePaths, tempFile.absolutePath).getOrThrow()

                // Copy temp to uri
                contentResolver.openOutputStream(outputUri)?.use { out ->
                    java.io.FileInputStream(tempFile).copyTo(out)
                }
                
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)
                onSuccess("PowerPoint Saved!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processPdfToImage(uri: Uri) {
         showProgress("Extracting Images...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // Copy PDF to cache for renderer
                val pdfFile = ScopedStorageHelper.copyUriToCache(this@ConverterActivity, uri, "pdf") 
                    ?: throw Exception("Could not access PDF")
                
                val renderer = android.graphics.pdf.PdfRenderer(
                    android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                )

                var count = 0
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        // Save Image
                        val fileName = "Page_${i+1}_${System.currentTimeMillis()}.jpg"
                        val imgUri = ScopedStorageHelper.createDocumentUri(
                            this@ConverterActivity, fileName, "image/jpeg"
                        )
                        
                        imgUri?.let { u ->
                            contentResolver.openOutputStream(u)?.use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            ScopedStorageHelper.finalizeFile(this@ConverterActivity, u)
                            count++
                        }
                        
                        bitmap.recycle()
                    }
                }
                renderer.close()

                onSuccess("Extracted $count Images!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processPdfToText(uri: Uri) {
         showProgress("Extracting Text...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // Copy PDF to cache
                val pdfFile = ScopedStorageHelper.copyUriToCache(this@ConverterActivity, uri, "pdf") 
                    ?: throw Exception("Could not access PDF")

                // Extract Text
                val text = com.scanner.lab.converters.TextConverter.pdfToText(pdfFile.absolutePath).getOrThrow()

                // 1. Save to TXT
                val txtFileName = "Extracted_${System.currentTimeMillis()}.txt"
                val txtUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, txtFileName, "text/plain"
                )
                txtUri?.let { u ->
                    contentResolver.openOutputStream(u)?.use { out ->
                        out.write(text.toByteArray())
                    }
                    ScopedStorageHelper.finalizeFile(this@ConverterActivity, u)
                }
                
                // 2. Save to DOCX using DocxConverter.textToDocx
                val docFileName = "Extracted_${System.currentTimeMillis()}.docx"
                val docUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, docFileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
                docUri?.let { u ->
                    val tempDocFile = ScopedStorageHelper.createCacheFile(this@ConverterActivity, "docx")
                    com.scanner.lab.converters.DocxConverter.textToDocx(text, tempDocFile.absolutePath).getOrThrow()
                    
                    contentResolver.openOutputStream(u)?.use { out ->
                        java.io.FileInputStream(tempDocFile).copyTo(out)
                    }
                    ScopedStorageHelper.finalizeFile(this@ConverterActivity, u)
                }

                onSuccess("Text Extracted to .txt and .docx!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    private fun processPdfToWord(uri: Uri) {
         showProgress("Converting to Word...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // Copy PDF to cache
                val pdfFile = ScopedStorageHelper.copyUriToCache(this@ConverterActivity, uri, "pdf") 
                    ?: throw Exception("Could not access PDF")

                // Convert PDF to DOCX using DocxConverter (OCR based layout)
                val tempDocFile = ScopedStorageHelper.createCacheFile(this@ConverterActivity, "docx")
                com.scanner.lab.converters.DocxConverter.pdfToDocx(pdfFile.absolutePath, tempDocFile.absolutePath).getOrThrow()

                // Save Document
                val fileName = "Converted_${System.currentTimeMillis()}.docx"
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ) ?: throw Exception("File creation failed")
                
                contentResolver.openOutputStream(outputUri)?.use { out ->
                    java.io.FileInputStream(tempDocFile).copyTo(out)
                }
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)

                onSuccess("Word Document Saved!")
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }
    
    private fun processPdfToPpt(uri: Uri) {
         showProgress("Converting to PowerPoint...")
        CoroutineScope(Dispatchers.IO).launch {
             try {
                // Copy PDF to cache
                val pdfFile = ScopedStorageHelper.copyUriToCache(this@ConverterActivity, uri, "pdf") 
                    ?: throw Exception("Could not access PDF")

                // Convert PDF to PPTX using PptxConverter (Text based slides)
                val tempPptFile = ScopedStorageHelper.createCacheFile(this@ConverterActivity, "pptx")
                com.scanner.lab.converters.PptxConverter.pdfToPptx(pdfFile.absolutePath, tempPptFile.absolutePath).getOrThrow()

                // Save Document
                val fileName = "Converted_${System.currentTimeMillis()}.pptx"
                val outputUri = ScopedStorageHelper.createDocumentUri(
                    this@ConverterActivity, 
                    fileName, 
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                ) ?: throw Exception("File creation failed")
                
                contentResolver.openOutputStream(outputUri)?.use { out ->
                    java.io.FileInputStream(tempPptFile).copyTo(out)
                }
                ScopedStorageHelper.finalizeFile(this@ConverterActivity, outputUri)

                onSuccess("PowerPoint Presentation Saved!")
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
