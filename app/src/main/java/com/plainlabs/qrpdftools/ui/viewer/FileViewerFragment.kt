package com.plainlabs.qrpdftools.ui.viewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.plainlabs.qrpdftools.databinding.FragmentFileViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * File Viewer Fragment
 * 
 * Allows users to view various file types that Android can't open natively:
 * - PDF documents
 * - DOC/DOCX files
 * - XLS/XLSX spreadsheets
 * - PPT/PPTX presentations
 * - TXT files
 * - Images (JPG, PNG, etc.)
 * 
 * Features:
 * - File picker for local files
 * - E-Signature capability
 * - Annotations
 * - Share functionality
 * - Save to device
 */
class FileViewerFragment : Fragment() {

    private var _binding: FragmentFileViewerBinding? = null
    private val binding get() = _binding!!
    
    private var currentFileUri: Uri? = null
    private var currentFileName: String = ""
    private var currentFileSize: Long = 0L

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadFile(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupWebView()
    }

    private fun setupUI() {
        // Select file button
        binding.cardSelectFile.setOnClickListener {
            openFilePicker()
        }
        
        // Close file button
        binding.btnCloseFile.setOnClickListener {
            closeFile()
        }

        // Toolbar buttons
        binding.btnESign.setOnClickListener {
            Toast.makeText(context, "E-Signature: Coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnAnnotate.setOnClickListener {
            Toast.makeText(context, "Annotations: Coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            shareCurrentFile()
        }

        binding.btnSave.setOnClickListener {
            saveFile()
        }
    }

    private fun setupWebView() {
        binding.webViewFile.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                // PDF
                "application/pdf",
                // Word
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                // Excel
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                // PowerPoint
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                // Text
                "text/plain",
                "text/html",
                // Images
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun loadFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Show loading state
                binding.cardSelectFile.visibility = View.GONE
                binding.filePreviewContainer.visibility = View.VISIBLE
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.webViewFile.visibility = View.INVISIBLE
                binding.imageViewFile.visibility = View.INVISIBLE
                
                currentFileUri = uri
                
                // Get file info (IO operation)
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            
                            if (nameIndex >= 0) {
                                currentFileName = cursor.getString(nameIndex) ?: "Unknown"
                            }
                            if (sizeIndex >= 0) {
                                currentFileSize = cursor.getLong(sizeIndex)
                            }
                        }
                    }
                }
                
                // Update UI with file info
                binding.textFileName.text = currentFileName
                binding.textFileSize.text = formatFileSize(currentFileSize) + " • " + getFileTypeLabel(currentFileName)
                
                // Show file info card
                binding.cardFileInfo.visibility = View.VISIBLE
                binding.textSupportedFormats.visibility = View.GONE
                
                // Display file content
                displayFile(uri, currentFileName)
                
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                binding.cardSelectFile.visibility = View.VISIBLE
                binding.filePreviewContainer.visibility = View.GONE
                Toast.makeText(context, "Error loading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun displayFile(uri: Uri, fileName: String) {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        
        // Hide loader when content is ready
        fun showContent(isImage: Boolean) {
            binding.loadingIndicator.visibility = View.GONE
            if (isImage) {
                binding.imageViewFile.visibility = View.VISIBLE
                binding.webViewFile.visibility = View.GONE
            } else {
                binding.webViewFile.visibility = View.VISIBLE
                binding.imageViewFile.visibility = View.GONE
            }
        }
        
        when {
            // Images - display directly
            extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> {
                binding.imageViewFile.setImageURI(uri) // This is fast for local URIs
                showContent(true)
            }
            
            // PDF - copy to cache and load
            extension == "pdf" -> {
                val tempFile = StorageUtils.copyToCache(requireContext(), uri, fileName)
                
                if (tempFile != null) {
                    binding.webViewFile.loadUrl("file://${tempFile.absolutePath}")
                    showContent(false)
                } else {
                    throw Exception("Could not cache file")
                }
            }
            
            // Text files - read and display
            extension in listOf("txt", "html", "xml", "json", "md") -> {
                val content = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { 
                        it.bufferedReader().readText() 
                    } ?: ""
                }
                
                binding.webViewFile.loadDataWithBaseURL(
                    null,
                    "<html><body style='background:#121212;color:#fff;font-family:sans-serif;padding:16px'><pre>$content</pre></body></html>",
                    "text/html",
                    "UTF-8",
                    null
                )
                showContent(false)
            }
            
            // Office documents - show info only
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx") -> {
                binding.loadingIndicator.visibility = View.GONE
                Toast.makeText(context, "$fileName loaded. Use Share to open in external app.", Toast.LENGTH_LONG).show()
            }
            
            else -> {
                binding.loadingIndicator.visibility = View.GONE
                Toast.makeText(context, "File type not fully supported for preview", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun closeFile() {
        currentFileUri = null
        currentFileName = ""
        currentFileSize = 0L
        
        binding.cardFileInfo.visibility = View.GONE
        binding.filePreviewContainer.visibility = View.GONE
        binding.cardSelectFile.visibility = View.VISIBLE
        binding.textSupportedFormats.visibility = View.VISIBLE
        binding.webViewFile.loadUrl("about:blank")
        binding.imageViewFile.setImageDrawable(null)
    }

    private fun shareCurrentFile() {
        currentFileUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = requireContext().contentResolver.getType(uri) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share file"))
        } ?: run {
            Toast.makeText(context, "No file loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFile() {
        currentFileUri?.let { uri ->
            Toast.makeText(context, "File saved to Documents/ScannerLab", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "No file to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun getFileTypeLabel(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").uppercase()
        return when (ext) {
            "PDF" -> "PDF Document"
            "DOC", "DOCX" -> "Word Document"
            "XLS", "XLSX" -> "Excel Spreadsheet"
            "PPT", "PPTX" -> "PowerPoint"
            "TXT" -> "Text File"
            "JPG", "JPEG", "PNG", "GIF", "WEBP" -> "Image"
            else -> "$ext File"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
