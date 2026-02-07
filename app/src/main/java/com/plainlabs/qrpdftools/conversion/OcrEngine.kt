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
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount
            val sb = StringBuilder()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                try {
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val task = recognizer.process(image)
                    val result = com.google.android.gms.tasks.Tasks.await(task)
                    
                    sb.append(result.text).append("\n\n")
                    callback((i + 1).toFloat() / pageCount, sb.toString())
                } finally {
                    page.close()
                    bitmap.recycle()
                }
            }
            callback(1.0f, sb.toString())

        } catch (e: Exception) {
            e.printStackTrace()
            callback(1.0f, "OCR Failed: ${e.message}")
        } finally {
            try { renderer?.close() } catch (e: Exception) {}
            try { fileDescriptor?.close() } catch (e: Exception) {}
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