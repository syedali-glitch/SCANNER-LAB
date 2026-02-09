package com.scanner.lab.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Privacy-First Scoped Storage Helper
 * Handles all File I/O compliantly for Android 10+ (Q) to Android 16 (Baklava).
 */
object ScopedStorageHelper {

    private const val AUTHORITY = "com.scanner.lab.fileprovider"
    
    /**
     * Create a new file Uri in MediaStore (Public Documents)
     */
    suspend fun createDocumentUri(context: Context, fileName: String, mimeType: String = "application/pdf"): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PlainLabsScanner")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        } else {
            // Legacy Storage for Android 9 (Pie) and below
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(docsDir, "PlainLabsScanner")
            if (!appDir.exists()) appDir.mkdirs()
            
            val file = File(appDir, fileName)
            // Return file Uri (or use FileProvider if sharing)
            // For internal writing, Uri.fromFile is okay if we handle it, 
            // but consistency suggests FileProvider or ContentResolver.
            // However, existing code expects a Uri to write to.
            // ContentResolver.openOutputStream(Uri.fromFile(file)) works!
            Uri.fromFile(file)
        }
    }
    
    /**
     * Finalize the file (make it visible to other apps)
     * Only needed for Android Q+
     */
    suspend fun finalizeFile(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            try {
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                // Ignore if update fails, file might already be visible
            }
        }
    }

    /**
     * Create a strictly internal cache file (for temporary processing)
     */
    fun createCacheFile(context: Context, extension: String = "tmp"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.cacheDir
        return File.createTempFile("TEMP_${timeStamp}_", ".$extension", storageDir)
    }

    /**
     * Get Uri for a private file (via FileProvider)
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }
    
    /**
     * Copy content from Uri to a local Cache File (Safe for processing with libraries requiring File path)
     */
    suspend fun copyUriToCache(context: Context, uri: Uri, extension: String = "pdf"): File? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = createCacheFile(context, extension)
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            null
        }
    }
}
