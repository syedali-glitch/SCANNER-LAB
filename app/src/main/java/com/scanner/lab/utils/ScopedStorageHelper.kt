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
    /**
     * Get filename from Uri
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) name = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }

    /**
     * Save Bitmap directly to Gallery
     */
    fun saveToGallery(context: Context, bitmap: android.graphics.Bitmap): Uri? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var imageUri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PlainLabsScanner")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        return try {
            imageUri = resolver.insert(collection, contentValues)
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
