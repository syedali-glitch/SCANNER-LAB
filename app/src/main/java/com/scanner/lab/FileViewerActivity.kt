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

class FileViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileViewerBinding
    private val fileAdapter = FileAdapter { file -> openFile(file) }
    
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
        loadDummyFiles() // For demo/verification, populate with some dummy entries or scan directory
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvFiles.layoutManager = LinearLayoutManager(this)
        binding.rvFiles.adapter = fileAdapter

        binding.fabPickFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain"))
        }
        
        // Chip Filters (Mock logic for now)
        binding.chipAll.setOnClickListener { filterList("ALL") }
        binding.chipPdf.setOnClickListener { filterList("PDF") }
        binding.chipDoc.setOnClickListener { filterList("DOC") }
        binding.chipXls.setOnClickListener { filterList("XLS") }
    }
    
    private fun filterList(type: String) {
        // Todo: Implement real filtering. For now just toast
        Toast.makeText(this, "Filtering: $type", Toast.LENGTH_SHORT).show()
    }

    private fun loadDummyFiles() {
        // Mock data to confirm UI works
        val mockFiles = listOf(
            FileModel("Scan_20240209.pdf", "2.4 MB", "PDF"),
            FileModel("Invoice_Oct.xlsx", "1.1 MB", "XLS"),
            FileModel("Contract_Draft.docx", "800 KB", "DOC"),
            FileModel("Notes.txt", "12 KB", "TXT")
        )
        fileAdapter.submitList(mockFiles)
    }

    private fun openFile(file: FileModel) {
        // TODO: Launch DocumentViewerActivity with dummy path or logic
        Toast.makeText(this, "Opening ${file.name}...", Toast.LENGTH_SHORT).show()
    }
    
    private fun openFileUri(uri: Uri) {
         // TODO: Launch DocumentViewerActivity with real URI
        Toast.makeText(this, "Opened: $uri", Toast.LENGTH_SHORT).show()
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

data class FileModel(val name: String, val size: String, val type: String)

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
                "PDF" -> binding.ivIcon.setImageResource(R.drawable.ic_pdf_tool) // reuse tool icon for now? or ic_picture_as_pdf if exists. Using generic doc for now.
                else -> binding.ivIcon.setImageResource(R.drawable.ic_scan_doc)
            }
        }
    }
}
