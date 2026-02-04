package com.plainlabs.qrpdftools.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.plainlabs.qrpdftools.conversion.ImageConverter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class OcrEngine(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // Synchronous/Blocking wrapper for compatibility if needed (but suspend is better)
    fun extractTextFromPdf(pdfFile: File, callback: (Float, String) -> Unit) {
        // Since we are in a non-suspend function in TextConverter currently (simplification),
        // we might need to block or launch a coroutine scope. 
        // For this restoration, we will use a naive blocking approach strictly for the logic flow, 
        // OR better, we assume the caller handles async/threading.
        // But TextConverter.convertPdfToText is likely called on Bg thread.
        // We'll execute logic here.
        
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount
            val sb = StringBuilder()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val image = InputImage.fromBitmap(bitmap, 0)
                // ML Kit process is async. We need to wait.
                // We'll use a latch or simple loop if in bg thread ??
                // Actually ML Kit Task API supports 'Tasks.await' which blocks.
                val task = recognizer.process(image)
                val result = com.google.android.gms.tasks.Tasks.await(task) // Blocks
                
                sb.append(result.text).append("\n\n")
                
                page.close()
                callback((i + 1).toFloat() / pageCount, sb.toString())
            }
            renderer.close()
            fileDescriptor.close()
            
            // Final callback with complete string is handled by progress updates?
            // Caller expects final string at end.
            callback(1.0f, sb.toString())

        } catch (e: Exception) {
            e.printStackTrace()
            callback(1.0f, "OCR Failed: ${e.message}")
        }
    }

    data class OcrResult(val text: String, val confidence: Float = 0.0f)

    companion object {
        fun performOcr(imagePath: String, context: Context): OcrResult {
            // Synchronous bridging for legacy/existing fragment calls
            //Ideally this should use the instance method but for static call compatibility:
            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath) ?: return OcrResult("")
            val instance = OcrEngine(context)
            val resultText: String
            
            // Blocking wait for the suspend function
            kotlinx.coroutines.runBlocking {
                resultText = instance.extractTextFromImage(bitmap)
            }
            return OcrResult(resultText)
        }
    }
}