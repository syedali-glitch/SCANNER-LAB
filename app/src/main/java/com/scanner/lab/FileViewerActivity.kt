package com.scanner.lab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class FileViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileViewerBinding
    private val fileAdapter = FileAdapter { file -> openFile(file) }
    
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
        
        // Scan Documents folder
        val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val scannerDir = File(docDir, "ScannerLab")
        
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
                allFiles.add(FileModel(file.name, size, ext, file.absolutePath))
            }
        }
        
        applyFilters()
    }

    private fun openFile(file: FileModel) {
        val uri = Uri.fromFile(File(file.path))
        openFileUri(uri)
    }
    
    private fun openFileUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}

data class FileModel(val name: String, val size: String, val type: String, val path: String)

class FileAdapter(private val onClick: (FileModel) -> Unit) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
    
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
            binding.tvFileSize.text = "${file.size} • ${file.type}"
            binding.root.setOnClickListener { onClick(file) }
            
            // Icon
            when(file.type) {
                "PDF" -> binding.ivIcon.setImageResource(R.drawable.ic_pdf_tool)
                "XLS", "XLSX" -> binding.ivIcon.setImageResource(R.drawable.ic_scan_doc) // Using scan icon as placeholder for xls
                "DOC", "DOCX" -> binding.ivIcon.setImageResource(R.drawable.ic_upload)
                else -> binding.ivIcon.setImageResource(R.drawable.ic_scan_doc)
            }
        }
    }
}
