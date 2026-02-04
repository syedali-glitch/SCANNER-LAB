package com.plainlabs.qrpdftools.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.plainlabs.qrpdftools.util.NativePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * ImageConverter - Refactored to use PlainLabs NativePdfGenerator.
 */
class ImageConverter(private val context: Context) {

    enum class ImageFormat {
        PNG, JPG, WEBP
    }

    /**
     * PDF to Image conversion using native PdfRenderer.
     */
    fun convertPdfToImages(pdfFile: File, outputDir: File, format: ImageFormat, @Suppress("UNUSED_PARAMETER") quality: Int, callback: (Float) -> Unit) {
        // ... (Existing native PdfRenderer logic is fine)
        try {
            val fileDescriptor = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val extension = when(format) {
                    ImageFormat.PNG -> "png"
                    ImageFormat.JPG -> "jpg"
                    ImageFormat.WEBP -> "webp"
                }
                
                val outputFile = File(outputDir, "page_${i + 1}.$extension")
                FileOutputStream(outputFile).use { out ->
                    val compressFormat = when(format) {
                        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                        ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
                        ImageFormat.WEBP -> {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                Bitmap.CompressFormat.WEBP_LOSSY
                            } else {
                                @Suppress("DEPRECATION")
                                Bitmap.CompressFormat.WEBP
                            }
                        }
                    }
                    bitmap.compress(compressFormat, quality, out)
                }
                
                page.close()
                callback((i + 1).toFloat() / pageCount)
            }
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            throw IOException("PDF to Images failed: ${e.message}")
        }
    }

    /**
     * Images to PDF conversion using memory-safe NativePdfGenerator.
     * Mandate: Offloaded to Dispatchers.IO.
     */
    suspend fun convertImagesToPdf(imageFiles: List<File>, outputPdf: File, @Suppress("UNUSED_PARAMETER") quality: Int, callback: (Float) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val result = NativePdfGenerator.generatePdfFromImages(imageFiles, outputPdf)
            if (result.isSuccess) {
                callback(1.0f)
            } else {
                throw result.exceptionOrNull() ?: IOException("Unknown error in NativePdfGenerator")
            }
        } catch (e: Exception) {
            throw IOException("Images to PDF failed: ${e.message}")
        }
    }
    
    fun convertImageFormat(inputFile: File, outputFile: File, format: ImageFormat, @Suppress("UNUSED_PARAMETER") quality: Int) {
        val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
        FileOutputStream(outputFile).use { out ->
            val compressFormat = when(format) {
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
                ImageFormat.WEBP -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
            }
            bitmap.compress(compressFormat, quality, out)
        }
    }
}