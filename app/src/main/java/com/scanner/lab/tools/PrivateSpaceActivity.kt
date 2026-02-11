package com.scanner.lab.tools

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.scanner.lab.BaseActivity
import com.scanner.lab.databinding.ActivityPrivateSpaceBinding
import com.scanner.lab.utils.ScopedStorageHelper
import java.io.File

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrivateSpaceActivity : BaseActivity() {
    // ... (rest of class)



    private lateinit var binding: ActivityPrivateSpaceBinding
    private val PREFS_NAME = "PrivateSpacePrefs"
    private val KEY_PIN = "user_pin"
    private val PRIVATE_DIR = "private_vault"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivateSpaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkPin()
    }

    private fun checkPin() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPin = prefs.getString(KEY_PIN, null)

        if (savedPin == null) {
            setupNewPin()
        } else {
            showPinEntry(savedPin)
        }
    }

    private fun setupNewPin() {
        binding.pinLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.tvPinTitle.text = "Set New PIN"
        binding.btnUnlock.text = "Set PIN"
        
        binding.btnUnlock.setOnClickListener {
            val pin = binding.etPin.text.toString()
            if (pin.length == 4) {
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_PIN, pin).apply()
                Toast.makeText(this, "PIN Set!", Toast.LENGTH_SHORT).show()
                showContent()
            } else {
                Toast.makeText(this, "Enter 4 digits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPinEntry(correctPin: String) {
        binding.pinLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.tvPinTitle.text = "Enter PIN"
        binding.btnUnlock.text = "Unlock"
        
        binding.btnUnlock.setOnClickListener {
            val pin = binding.etPin.text.toString()
            if (pin == correctPin) {
                showContent()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                binding.etPin.text.clear()
            }
        }
    }

    private fun showContent() {
        binding.pinLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        
        setupUI()
        loadPrivateFiles()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnAddFile.setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }
    }
    
    private val filePicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFile(it) }
    }

    private fun importFile(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val fileName = ScopedStorageHelper.getFileName(this, uri) ?: "imported_file"
            // Get private directory
            val privateDir = File(filesDir, PRIVATE_DIR)
            if (!privateDir.exists()) privateDir.mkdirs()
            
            val destFile = File(privateDir, fileName)
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "File Imported to Vault", Toast.LENGTH_SHORT).show()
            loadPrivateFiles()
        } catch (e: Exception) {
             Toast.makeText(this, "Import Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPrivateFiles() {
        val privateDir = File(filesDir, PRIVATE_DIR)
        if (!privateDir.exists()) privateDir.mkdirs()
        
        val files = privateDir.listFiles()?.toList() ?: emptyList()
        
        if (files.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            // Use a simple adapter or reuse FileAdapter?
            // For simplicity in v1.4, just listing filenames in a simple text list
            // Or quickly creating a minimal adapter here.
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = PrivateFileAdapter(files) { file ->
                // On Click: Show options (Export/Delete)
                showFileOptions(file)
            }
        }
    }
    
    private fun showFileOptions(file: File) {
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(arrayOf("Export to Gallery/Docs", "Delete")) { _, which ->
                when (which) {
                    0 -> exportFile(file)
                    1 -> {
                        file.delete()
                        loadPrivateFiles()
                    }
                }
            }
            .show()
    }
    
    private fun exportFile(file: File) {
        // Simple export: Copy to public Documents
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
             try {
                val mimeType = if (file.extension == "pdf") "application/pdf" else "image/jpeg"
                val uri = ScopedStorageHelper.createDocumentUri(this@PrivateSpaceActivity, file.name, mimeType)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                    ScopedStorageHelper.finalizeFile(this@PrivateSpaceActivity, uri)
                    runOnUiThread {
                        Toast.makeText(this@PrivateSpaceActivity, "Exported successfully", Toast.LENGTH_SHORT).show()
                    }
                }
             } catch (e: Exception) {
                 runOnUiThread {
                     Toast.makeText(this@PrivateSpaceActivity, "Export Failed", Toast.LENGTH_SHORT).show()
                 }
             }
        }
    }
    
    // Minimal Adapter
    class PrivateFileAdapter(private val files: List<File>, private val onClick: (File) -> Unit) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<PrivateFileAdapter.ViewHolder>() {
            
        class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvName: android.widget.TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            view.setBackgroundResource(android.R.drawable.list_selector_background) // Simple touch feedback
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
             holder.tvName.text = file.name
             holder.tvName.setTextColor(android.graphics.Color.WHITE) // Dark theme
             holder.itemView.setOnClickListener { onClick(file) }
        }

        override fun getItemCount() = files.size
    }
}
