package com.plainlabs.qrpdftools.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * StorageUtils - PlainLabs Architecture Guardrail.
 * 
 * Handles SAF-compliant file operations and caching.
 */
object StorageUtils {

    /**
     * Copies a file from a content Uri to the app's internal cache.
     * Essential for processing files before conversion.
     */
    suspend fun copyToCache(context: Context, uri: Uri, fileName: String? = null): File? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val name = fileName ?: getFileName(context, uri) ?: "temp_file"
            val tempFile = File(context.cacheDir, name)
            
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    /**
     * Cleans up all files in the cache directory.
     */
    fun clearCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.delete() }
    }
}
