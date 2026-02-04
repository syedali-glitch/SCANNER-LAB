package com.plainlabs.qrpdftools.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * NativePdfGenerator - Proprietary PDF engine for PlainLabs Scanner.
 * 
 * Uses Android's native PdfDocument for iText-free generation.
 * Implements strict memory safeguards mandated by the Senior Architect:
 * 1. Image downsampling to 2048px cap.
 * 2. Explicit bitmap recycling after page draw.
 * 3. Thread-safe I/O on Dispatchers.IO.
 */
object NativePdfGenerator {

    private const val MAX_DIMENSION = 2048
    private const val JPEG_QUALITY = 85

    /**
     * Converts a list of image files into a single PDF document.
     * Uses Dynamic Sizing: each page size matches its image aspect ratio.
     */
    suspend fun generatePdfFromImages(imageFiles: List<File>, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        
        try {
            imageFiles.forEach { file ->
                val bitmap = loadMemorySafeBitmap(file) ?: return@forEach
                
                // Dynamic Sizing: Use bitmap dimensions for the page
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pdfDocument.pages.size + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                
                val canvas = page.canvas
                val paint = Paint()
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                
                pdfDocument.finishPage(page)
                
                // Architect Rule: Explicitly recycle immediately after draw
                bitmap.recycle()
            }
            
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Loads a bitmap with architect-mandated downsampling (2048px cap).
     */
    private fun loadMemorySafeBitmap(file: File): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION)
        options.inJustDecodeBounds = false
        
        return try {
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Standard utility to calculate sub-sampling factor to fit dimensions.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
