package com.plainlabs.qrpdftools.conversion

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OptimizedConversionEngine(private val context: Context) {
    
    // Cache for OCR results
    private val ocrCache = LruCache<String, String>(100) // cache 100 items

    suspend fun batchImagesToPdf(images: List<File>, outputPdf: File, callback: (Float) -> Unit) {
        val imageConverter = ImageConverter(context)
        // Optimized call
        withContext(Dispatchers.Default) {
             imageConverter.convertImagesToPdf(images, outputPdf, 80) { progress ->
                 callback(progress)
             }
        }
    }

    suspend fun convertWithCache(key: String, conversionAction: suspend () -> String): String {
        return ocrCache.get(key) ?: run {
            val result = conversionAction()
            ocrCache.put(key, result)
            result
        }
    }
}