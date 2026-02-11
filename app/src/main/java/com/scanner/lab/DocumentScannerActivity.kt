package com.scanner.lab

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scanner.lab.converters.TextConverter
import com.scanner.lab.converters.DocxConverter
import com.scanner.lab.converters.ExcelGenerator
import com.scanner.lab.databinding.ActivityDocumentScannerBinding
import com.scanner.lab.ui.ScannerOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Document Scanner Activity with OCR
 */
class DocumentScannerActivity : BaseActivity() {
    companion object {
        const val EXTRA_SCAN_MODE = "scan_mode"
    }

    private lateinit var binding: ActivityDocumentScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var capturedBitmaps: MutableList<Bitmap> = mutableListOf()
    // ID Card State
    private var currentScanMode = ScannerOverlayView.ScanMode.DOCUMENT
    private var idCardFrontBitmap: Bitmap? = null
    private var isAioMode = false
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge Polish
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityDocumentScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Determine Mode
        val modeOrdinal = intent.getIntExtra(EXTRA_SCAN_MODE, ScannerOverlayView.ScanMode.DOCUMENT.ordinal)
        currentScanMode = ScannerOverlayView.ScanMode.values().getOrElse(modeOrdinal) { ScannerOverlayView.ScanMode.DOCUMENT }
        isAioMode = intent.getBooleanExtra("is_aio_mode", false)

        // Set Scanner Overlay
        binding.scannerOverlay.setScanMode(currentScanMode)
        
        // Apply Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // ... (Existing init code) ...
        // Init Monetization
        com.scanner.lab.utils.UserPremiums.init(this)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupUI()
        checkCameraPermission()
    }
    
    // ... (Permission Launchers) ...
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    private fun setupUI() {
        // Back Button
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnCapture.setOnClickListener { captureImage() }
        
        binding.btnRetake.setOnClickListener {
            // Reset to Capture Mode
            resetToCameraMode()
        }
        
        binding.btnSave.setOnClickListener { saveDocument() }
    }
    
    private fun resetToCameraMode() {
        binding.btnCapture.visibility = View.VISIBLE
        binding.btnSaveContainer.visibility = View.GONE
        binding.btnRetakeContainer.visibility = View.GONE
        binding.imgPreview.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        
        capturedBitmaps.forEach { it.recycle() }
        capturedBitmaps.clear()
        
        idCardFrontBitmap?.recycle()
        idCardFrontBitmap = null
        
        // If ID Card mode, toast hint
        if (currentScanMode == ScannerOverlayView.ScanMode.ID_CARD) {
            Toast.makeText(this, "Align FRONT of ID Card", Toast.LENGTH_SHORT).show()
        }
    }

    // ... (Start Camera) ...
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("DocScanner", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        
        // Feedback
        binding.btnCapture.isEnabled = false
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    handleCapture(bitmap)
                    binding.btnCapture.isEnabled = true
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e("DocScanner", "Capture failed: ${exception.message}", exception)
                    binding.btnCapture.isEnabled = true
                }
            }
        )
    }
    
    private fun handleCapture(bitmap: Bitmap) {
        if (currentScanMode == ScannerOverlayView.ScanMode.BOOK) {
            // Book Mode: Split Logic
            binding.progressLayout.visibility = View.VISIBLE
             CoroutineScope(Dispatchers.Default).launch {
                 val split = com.scanner.lab.processors.BookPageProcessor.splitPages(bitmap)
                 bitmap.recycle() // Recycle original full input
                 
                 withContext(Dispatchers.Main) {
                     capturedBitmaps.add(split.first) // Left Page
                     capturedBitmaps.add(split.second) // Right Page
                     binding.progressLayout.visibility = View.GONE
                     Toast.makeText(this@DocumentScannerActivity, "Pages Split Successfully!", Toast.LENGTH_SHORT).show()
                     showPreview() // Will show the first page (Left)
                 }
             }
        } else if (currentScanMode == ScannerOverlayView.ScanMode.ID_CARD) {
            if (idCardFrontBitmap == null) {
                // First Capture (Front)
                idCardFrontBitmap = bitmap
                Toast.makeText(this, "Front Captured. Now Align BACK.", Toast.LENGTH_LONG).show()
                // Do NOT switch to preview yet. Stay in camera.
                // Optionally play a sound or animation.
            } else {
                // Second Capture (Back) -> Stitch
                binding.progressLayout.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Default).launch {
                    val combined = com.scanner.lab.processors.IdCardProcessor.stitchImages(idCardFrontBitmap!!, bitmap)
                    
                    // Cleanup inputs
                    bitmap.recycle() // Recycle Back immediately
                    // idCardFrontBitmap will be recycled in reset or onDestroy
                    
                    withContext(Dispatchers.Main) {
                        capturedBitmaps.add(combined)
                        binding.progressLayout.visibility = View.GONE
                        showPreview()
                    }
                }
            }
        } else if (currentScanMode == ScannerOverlayView.ScanMode.PASSPORT) {
            // Passport Mode: OCR -> MRZ Extraction
            binding.progressLayout.visibility = View.VISIBLE
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    binding.progressLayout.visibility = View.GONE
                    val rawText = visionText.text
                    // Simple MRZ Filter: Look for lines with <<
                    val mrzLines = rawText.split("\n").filter { it.contains("<<") }
                    
                    val message = if (mrzLines.isNotEmpty()) {
                        "MRZ Data Detected:\n\n${mrzLines.joinToString("\n")}"
                    } else {
                        "No MRZ data detected.\n\nEnsure the passport bottom zone is clear and well-lit."
                    }
                    
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Passport Scanned")
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ -> 
                            // Resume Camera
                            binding.btnCapture.isEnabled = true
                        }
                        .setOnDismissListener {
                            binding.btnCapture.isEnabled = true
                        }
                        .show()
                        
                    // Recycle bitmap as we don't save it in this mode (v1.1.0)
                    bitmap.recycle()
                }
                .addOnFailureListener { e ->
                    binding.progressLayout.visibility = View.GONE
                    Toast.makeText(this, "OCR Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnCapture.isEnabled = true
                    bitmap.recycle()
                }
        } else {
            // Standard Document Mode
            capturedBitmaps.add(bitmap)
            showPreview()
        }
    }
    
    private fun showPreview() {
        if (capturedBitmaps.isEmpty()) return
        
        binding.viewFinder.visibility = View.GONE
        binding.imgPreview.visibility = View.VISIBLE
        // Show the last captured image (or first? Usually last is user friendly)
        // For Book Mode, maybe show Left page? Let's show the last added.
        binding.imgPreview.setImageBitmap(capturedBitmaps.last())
        
        binding.btnCapture.visibility = View.GONE
        binding.btnSaveContainer.visibility = View.VISIBLE
        binding.btnRetakeContainer.visibility = View.VISIBLE
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = android.graphics.Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // --- Smart Save AIO Flow ---
    private fun showSmartSaveSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_save_format, null)
        dialog.setContentView(sheetView)

        // 1. PDF
        sheetView.findViewById<View>(R.id.btnFormatPdf)?.setOnClickListener {
            dialog.dismiss()
            processSmartSave("PDF")
        }
        
        // 2. Word
        sheetView.findViewById<View>(R.id.btnFormatWord)?.setOnClickListener {
            dialog.dismiss()
            processSmartSave("WORD")
        }
        
        // 3. Excel
        sheetView.findViewById<View>(R.id.btnFormatExcel)?.setOnClickListener {
            dialog.dismiss()
            processSmartSave("EXCEL")
        }
        
        // 4. Text
        sheetView.findViewById<View>(R.id.btnFormatText)?.setOnClickListener {
            dialog.dismiss()
            processSmartSave("TEXT")
        }
        
        // 5. Image
        sheetView.findViewById<View>(R.id.btnFormatImage)?.setOnClickListener {
            dialog.dismiss()
            processSmartSave("IMAGE")
        }
        
        dialog.show()
    }
    
    private fun processSmartSave(format: String) {
        binding.progressLayout.visibility = View.VISIBLE
        setButtonsEnabled(false)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Pre-process bitmaps if needed (Magic Filter)
                val processedBitmaps = preprocessBitmaps()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                
                var savedFileUri: android.net.Uri? = null
                var mimeType = "application/pdf"
                
                when (format) {
                    "PDF" -> {
                        val fileName = "Scan_$timestamp.pdf"
                        savedFileUri = com.scanner.lab.utils.ScopedStorageHelper.createDocumentUri(this@DocumentScannerActivity, fileName, "application/pdf")
                        if (savedFileUri != null) {
                            val pageUris = saveBitmapsToCache(processedBitmaps)
                            com.scanner.lab.converters.NativePdfGenerator.generatePdf(this@DocumentScannerActivity, pageUris, savedFileUri).getOrThrow()
                        }
                        mimeType = "application/pdf"
                    }
                    "WORD" -> {
                         val fileName = "Scan_$timestamp.docx"
                         savedFileUri = com.scanner.lab.utils.ScopedStorageHelper.createDocumentUri(this@DocumentScannerActivity, fileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                         if (savedFileUri != null) {
                             val pageUris = saveBitmapsToCache(processedBitmaps)
                             // Convert Uris to Paths
                             val imagePaths = pageUris.mapNotNull { com.scanner.lab.utils.ScopedStorageHelper.copyUriToCache(this@DocumentScannerActivity, it, "jpg")?.absolutePath }
                             val tempDoc = com.scanner.lab.utils.ScopedStorageHelper.createCacheFile(this@DocumentScannerActivity, "docx")
                             DocxConverter.imagesToDocx(imagePaths, tempDoc.absolutePath).getOrThrow()
                             // Copy to final
                             contentResolver.openOutputStream(savedFileUri)?.use { out ->
                                 java.io.FileInputStream(tempDoc).copyTo(out)
                             }
                         }
                         mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    }
                    "EXCEL" -> {
                        val fileName = "Scan_$timestamp.xlsx"
                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        savedFileUri = com.scanner.lab.utils.ScopedStorageHelper.createDocumentUri(this@DocumentScannerActivity, fileName, mimeType)
                        
                        if (savedFileUri != null) {
                            // Run OCR
                            val texts = runOcrOnBitmaps(processedBitmaps)
                            contentResolver.openOutputStream(savedFileUri)?.use { out ->
                                ExcelGenerator.generateExcel(this@DocumentScannerActivity, texts, out).getOrThrow()
                            }
                        }
                    }
                    "TEXT" -> {
                        val fileName = "Scan_$timestamp.txt"
                         mimeType = "text/plain"
                        savedFileUri = com.scanner.lab.utils.ScopedStorageHelper.createDocumentUri(this@DocumentScannerActivity, fileName, mimeType)
                        
                        if (savedFileUri != null) {
                             // Run OCR
                            val texts = runOcrOnBitmaps(processedBitmaps)
                            val fullText = texts.joinToString("\n\n--- Page Break ---\n\n")
                            contentResolver.openOutputStream(savedFileUri)?.use { out ->
                                out.write(fullText.toByteArray())
                            }
                        }
                    }
                    "IMAGE" -> {
                        // Save to Gallery (All images)
                        mimeType = "image/jpeg"
                        // We will save all, but only share the last one or create a zip? 
                        // User request: "save to local directory". Usually for "Image" it means save to gallery.
                        // Let's save all to gallery and share the last one (simplification) OR share multiple if supported.
                        // For now, let's implement saving all to gallery.
                        val uris = saveBitmapsToGallery(processedBitmaps)
                        if (uris.isNotEmpty()) savedFileUri = uris.last() // Share the last one for now to keep single stream logic
                        // If we want to share multiple, we need ArrayList<Uri> intent.
                        // Let's stick to single file logic for now or Zip? "Image" format usually implies "Save as JPGs".
                        // Let's assume user wants to share the collection.
                        if (uris.size > 1) {
                            // If multiple, maybe just toast "Saved to Gallery" and share the first?
                            // Implementing robust multi-share is complex here. Let's start with saving to gallery.
                        }
                    }
                }

                if (savedFileUri != null) {
                    com.scanner.lab.utils.ScopedStorageHelper.finalizeFile(this@DocumentScannerActivity, savedFileUri)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DocumentScannerActivity, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                        shareFile(savedFileUri, mimeType)
                        finish()
                    }
                } else {
                     throw Exception("Save Failed")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DocumentScannerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressLayout.visibility = View.GONE
                    setButtonsEnabled(true)
                }
            }
        }
    }
    
    private suspend fun preprocessBitmaps(): List<Bitmap> {
        return capturedBitmaps.map { bitmap ->
            val isEnhancedMode = (currentScanMode == ScannerOverlayView.ScanMode.DOCUMENT || currentScanMode == ScannerOverlayView.ScanMode.BOOK)
            if (isEnhancedMode) {
                 com.scanner.lab.utils.ImageProcessor.applyFilter(bitmap, com.scanner.lab.utils.ImageProcessor.FilterMode.MAGIC)
            } else {
                bitmap
            }
        }
    }
    
    private fun saveBitmapsToCache(bitmaps: List<Bitmap>): List<android.net.Uri> {
        val uris = mutableListOf<android.net.Uri>()
        bitmaps.forEach { bmp ->
            val file = com.scanner.lab.utils.ScopedStorageHelper.createCacheFile(this, "jpg")
            FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            uris.add(com.scanner.lab.utils.ScopedStorageHelper.getUriForFile(this, file))
        }
        return uris
    }
    
    // Save to Public Gallery
    private fun saveBitmapsToGallery(bitmaps: List<Bitmap>): List<android.net.Uri> {
         val uris = mutableListOf<android.net.Uri>()
         bitmaps.forEach { bmp ->
             val uri = com.scanner.lab.utils.ScopedStorageHelper.saveToGallery(this, bmp)
             if (uri != null) uris.add(uri)
         }
         return uris
    }

    private suspend fun runOcrOnBitmaps(bitmaps: List<Bitmap>): List<String> {
        val texts = mutableListOf<String>()
        bitmaps.forEach { bmp ->
            try {
                val input = InputImage.fromBitmap(bmp, 0)
                val result = com.google.android.gms.tasks.Tasks.await(textRecognizer.process(input))
                texts.add(result.text)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return texts
    }

    private fun shareFile(uri: android.net.Uri, mimeType: String) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share via..."))
    }

    private fun saveDocument() {
        if (capturedBitmaps.isEmpty()) return

        if (isAioMode) {
            showSmartSaveSheet()
            return
        }
        
        binding.progressLayout.visibility = View.VISIBLE
        setButtonsEnabled(false)
        
        val bitmapsToSave = ArrayList(capturedBitmaps) // Copy list
        val modeSnapshot = currentScanMode // Capture mode
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "scan_$timestamp.pdf"
                
                val pageUris = mutableListOf<android.net.Uri>()
                
                // Process each bitmap
                for ((index, bitmap) in bitmapsToSave.withIndex()) {
                    // Determine if we need to process (and thus explicitly recycle) the bitmap
                    // Use local modeSnapshot
                    val isEnhancedMode = (modeSnapshot == ScannerOverlayView.ScanMode.DOCUMENT || modeSnapshot == ScannerOverlayView.ScanMode.BOOK)
                    
                    val finalBitmap = if (isEnhancedMode) {
                         com.scanner.lab.utils.ImageProcessor.applyFilter(bitmap, com.scanner.lab.utils.ImageProcessor.FilterMode.MAGIC)
                    } else {
                        bitmap
                    }
                    
                    val cacheFile = com.scanner.lab.utils.ScopedStorageHelper.createCacheFile(this@DocumentScannerActivity, "jpg")
                    FileOutputStream(cacheFile).use { out ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    
                    // if (isEnhancedMode) {
                    //    finalBitmap.recycle()
                    // }
                    
                    val imageUri = com.scanner.lab.utils.ScopedStorageHelper.getUriForFile(this@DocumentScannerActivity, cacheFile)
                    pageUris.add(imageUri)
                }
                
                val targetUri = com.scanner.lab.utils.ScopedStorageHelper.createDocumentUri(this@DocumentScannerActivity, fileName)
                    ?: throw Exception("Could not create output file")
                    
                val success = com.scanner.lab.converters.NativePdfGenerator.generatePdf(
                    this@DocumentScannerActivity, 
                    pageUris, 
                    targetUri
                )
                
                if (success.isSuccess) {
                    com.scanner.lab.utils.ScopedStorageHelper.finalizeFile(this@DocumentScannerActivity, targetUri)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DocumentScannerActivity, "Saved ${pageUris.size} pages to PDF", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    throw Exception("PDF Generation failed")
                }
                // Cleanup Cache? (Optional, cacheDir is cleared by OS eventually, but good practice)
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DocumentScannerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressLayout.visibility = View.GONE
                    setButtonsEnabled(true)
                }
            }
        }
    }
    
    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnCapture.isEnabled = enabled
        binding.btnSave.isEnabled = enabled
        binding.btnRetake.isEnabled = enabled
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textRecognizer.close()
        capturedBitmaps.forEach { it.recycle() }
        idCardFrontBitmap?.recycle()
    }
}
