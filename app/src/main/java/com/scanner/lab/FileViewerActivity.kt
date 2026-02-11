package com.scanner.lab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scanner.lab.databinding.ActivityFileViewerBinding
import com.scanner.lab.databinding.ItemFilePremiumBinding
import java.io.File
import java.util.Locale

class FileViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityFileViewerBinding
    private val fileAdapter = FileAdapter(
        onClick = { file -> openFile(file) },
        onMoreClick = { _, file -> showContextualSheet(file) }
    )
    
    private var allFiles = mutableListOf<FileModel>()
    private var currentFilter = "ALL"
    private var currentQuery = ""

    // File Picker
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            openFileUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityFileViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        scanFiles()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvFiles.layoutManager = LinearLayoutManager(this)
        binding.rvFiles.adapter = fileAdapter

        binding.fabPickFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain"))
        }
        
        // Search Logic
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.lowercase() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Chip Filters
        binding.chipAll.setOnClickListener { selectFilter("ALL") }
        binding.chipPdf.setOnClickListener { selectFilter("PDF") }
        binding.chipDoc.setOnClickListener { selectFilter("DOC") }
        binding.chipXls.setOnClickListener { selectFilter("XLS") }
    }
    
    private fun selectFilter(type: String) {
        currentFilter = type
        // Update chip UI (Optional: change colors to show active state)
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allFiles.filter { file ->
            val matchesType = when (currentFilter) {
                "PDF" -> file.type == "PDF"
                "DOC" -> file.type == "DOC" || file.type == "DOCX"
                "XLS" -> file.type == "XLS" || file.type == "XLSX"
                else -> true
            }
            val matchesQuery = file.name.lowercase().contains(currentQuery)
            matchesType && matchesQuery
        }
        fileAdapter.submitList(filtered)
    }

    private fun scanFiles() {
        allFiles.clear()
        
        // Scan Documents folder for PlainLabsScanner
        val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val scannerDir = File(docDir, "PlainLabsScanner")
        
        val filesToScan = mutableListOf<File>()
        if (scannerDir.exists()) {
            scannerDir.listFiles()?.let { filesToScan.addAll(it) }
        }
        
        // Also scan internal files if any
        getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.listFiles()?.let { filesToScan.addAll(it) }

        filesToScan.forEach { file ->
            if (file.isFile) {
                val ext = file.extension.uppercase()
                val size = String.format("%.1f MB", file.length() / (1024.0 * 1024.0))
                allFiles.add(FileModel(file.name, size, ext, file.absolutePath, file.lastModified()))
            }
        }
        
        // Sort by Date Modified (Newest First)
        allFiles.sortByDescending { it.dateModified }
        
        applyFilters()
    }

    private fun openFile(file: FileModel) {
        val uri = Uri.fromFile(File(file.path))
        openFileUri(uri)
    }
    
    private fun openFileUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, getContentResolver().getType(uri) ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showContextualSheet(file: FileModel) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.scanner.lab.databinding.BottomSheetFileContextBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        
        // Header Info
        sheetBinding.tvSheetName.text = file.name
        sheetBinding.tvSheetSize.text = file.size
        
        // Icon & Actions Logic
        val isPdf = file.type.equals("PDF", ignoreCase = true)
        val isImage = file.type.equals("JPG", ignoreCase = true) || file.type.equals("PNG", ignoreCase = true)
        
        if (isPdf) {
            sheetBinding.ivSheetIcon.setImageResource(R.drawable.ic_pdf_tool)
            sheetBinding.llPdfActions.visibility = android.view.View.VISIBLE
            sheetBinding.llImageActions.visibility = android.view.View.GONE
            
            // PDF Actions
            sheetBinding.btnActionSign.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, com.scanner.lab.tools.SignatureActivity::class.java))
            }
            sheetBinding.btnActionCompress.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(this, PdfToolsActivity::class.java)
                intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_COMPRESS)
                startActivity(intent)
            }
            sheetBinding.btnActionEncrypt.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(this, PdfToolsActivity::class.java)
                intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_PASSWORD)
                startActivity(intent)
            }
        } else if (isImage) {
            sheetBinding.ivSheetIcon.setImageResource(R.drawable.ic_scan_doc)
            sheetBinding.llPdfActions.visibility = android.view.View.GONE
            sheetBinding.llImageActions.visibility = android.view.View.VISIBLE
            
            // Image Actions
            sheetBinding.btnActionConvertToPdf.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(this, ConverterActivity::class.java)
                intent.putExtra(ConverterActivity.EXTRA_CONVERSION_MODE, ConverterActivity.MODE_IMG_TO_PDF)
                startActivity(intent)
            }
            sheetBinding.btnActionOcr.setOnClickListener {
                dialog.dismiss()
                // Default to OCR
                startActivity(Intent(this, ConverterActivity::class.java))
            }
        } else {
            // Other types (Word, Excel) -> No specific actions, or default
            sheetBinding.ivSheetIcon.setImageResource(R.drawable.ic_upload)
            sheetBinding.llPdfActions.visibility = android.view.View.GONE
            sheetBinding.llImageActions.visibility = android.view.View.GONE
        }
        
        // Common Actions
        sheetBinding.btnCommonOpen.setOnClickListener {
            dialog.dismiss()
            openFile(file)
        }
        sheetBinding.btnCommonShare.setOnClickListener {
            dialog.dismiss()
            shareFile(file)
        }
        sheetBinding.btnCommonPrint.setOnClickListener {
             dialog.dismiss()
             printFile(file)
        }
        sheetBinding.btnCommonDelete.setOnClickListener {
             dialog.dismiss()
             deleteFile(file)
        }
        sheetBinding.btnMoreTools.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ToolsActivity::class.java))
        }

        dialog.show()
    }

    private fun shareFile(file: FileModel) {
        val uri = com.scanner.lab.utils.ScopedStorageHelper.getUriForFile(this, File(file.path))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*" // Or specific mime type
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share File"))
    }

    private fun printFile(file: FileModel) {
        try {
            val printManager = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "${getString(R.string.app_name)} Document"
            
            // Native PDF Printing
            if (file.type == "PDF") {
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: android.print.PrintAttributes?,
                        newAttributes: android.print.PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(file.name)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                         try {
                            val input = java.io.FileInputStream(file.path)
                            val output = java.io.FileOutputStream(destination?.fileDescriptor)
                            input.copyTo(output)
                            input.close()
                            output.close()
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(jobName, printAdapter, null)
            } else if (file.type == "JPG" || file.type == "PNG") {
                // Image Printing
                 androidx.print.PrintHelper(this).apply {
                    scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
                }.printBitmap(jobName, android.net.Uri.fromFile(File(file.path)))
            } else {
                Toast.makeText(this, "Printing not supported for this file type", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
             Toast.makeText(this, "Print Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFile(file: FileModel) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val f = File(file.path)
                if (f.delete()) {
                    Toast.makeText(this, "File Deleted", Toast.LENGTH_SHORT).show()
                    scanFiles()
                } else {
                    Toast.makeText(this, "Could not delete file", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}

data class FileModel(val name: String, val size: String, val type: String, val path: String, val dateModified: Long)

class FileAdapter(
    private val onClick: (FileModel) -> Unit,
    private val onMoreClick: (android.view.View, FileModel) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    
    private var files = listOf<FileModel>()

    fun submitList(newFiles: List<FileModel>) {
        files = newFiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFilePremiumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount() = files.size

    inner class FileViewHolder(private val binding: ItemFilePremiumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileModel) {
            binding.tvFileName.text = file.name
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            val dateStr = dateFormat.format(java.util.Date(file.dateModified))
            binding.tvFileSize.text = "${file.size} • $dateStr"
            
            // Main click -> Open
            binding.root.setOnClickListener { onClick(file) }
            
            // More click -> Menu
            binding.btnMore.setOnClickListener { onMoreClick(it, file) }
            
            // Icon
            when(file.type) {
                "PDF" -> binding.ivIcon.setImageResource(R.drawable.ic_pdf_tool)
                "XLS", "XLSX" -> binding.ivIcon.setImageResource(R.drawable.ic_scan_doc) 
                "DOC", "DOCX" -> binding.ivIcon.setImageResource(R.drawable.ic_upload)
                else -> binding.ivIcon.setImageResource(R.drawable.ic_scan_doc)
            }
        }
    }
}
