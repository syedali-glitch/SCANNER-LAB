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
import com.scanner.lab.databinding.ActivityDocumentScannerBinding
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
class DocumentScannerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDocumentScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var capturedBitmap: Bitmap? = null
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupUI()
        startCamera()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnCapture.setOnClickListener {
            captureImage()
        }
        
        binding.btnRetake.setOnClickListener {
            binding.layoutActions.visibility = View.GONE
            binding.btnCapture.visibility = View.VISIBLE
            binding.cardInstructions.visibility = View.VISIBLE
            capturedBitmap?.recycle()
            capturedBitmap = null
        }
        
        binding.btnSave.setOnClickListener {
            saveDocument()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("DocScanner", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        
        binding.btnCapture.animatePulse()
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    capturedBitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    runOnUiThread {
                        binding.btnCapture.visibility = View.GONE
                        binding.cardInstructions.visibility = View.GONE
                        binding.layoutActions.visibility = View.VISIBLE
                        binding.layoutActions.startAnimation(
                            android.view.animation.AnimationUtils.loadAnimation(
                                this@DocumentScannerActivity,
                                R.anim.slide_in_bottom
                            )
                        )
                        binding.btnSave.animateSuccess()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e("DocScanner", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(
                        this@DocumentScannerActivity,
                        "Capture failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
    
    private fun saveDocument() {
        val bitmap = capturedBitmap ?: return
        
        binding.btnSave.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Perform OCR
                val image = InputImage.fromBitmap(bitmap, 0)
                val text = withContext(Dispatchers.Main) {
                    textRecognizer.process(image)
                }.await().text
                
                // Save as PDF
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "scan_$timestamp"
                val outputDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val pdfFile = File(outputDir, "$fileName.pdf")
                
                TextConverter.textToPdf(text, pdfFile.absolutePath)
                
                // Also save the image
                val imageFile = File(outputDir, "$fileName.jpg")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DocumentScannerActivity,
                        getString(R.string.success_saved),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("DocScanner", "Save failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DocumentScannerActivity,
                        "Save failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textRecognizer.close()
        capturedBitmap?.recycle()
    }
}

// Extension function for Task<T>.await()
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result) {}
        }
        addOnFailureListener { exception ->
            cont.cancel(exception)
        }
        cont.invokeOnCancellation {
            // Task cannot be cancelled, but we handle this for cleanup
        }
    }
}
