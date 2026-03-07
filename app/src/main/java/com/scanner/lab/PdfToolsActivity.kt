package com.scanner.lab

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.scanner.lab.databinding.ActivityPdfToolsBinding
import com.scanner.lab.utils.PdfUtilityTools
import com.scanner.lab.utils.ScopedStorageHelper
import com.scanner.lab.utils.UserPremiums
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfToolsActivity : BaseActivity() {

    private lateinit var binding: ActivityPdfToolsBinding
    private var isMerging = false
    
    // File Picker for Multiple PDFs (Merge)
    private val mergePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            performMerge(uris)
        }
    }
    
    // FILE PICKERS
    private val splitPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { performSplit(it) } }
    private val compressPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { showCompressDialog(it) } }
    private val watermarkPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { showWatermarkDialog(it) } }
    private val passwordPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { showPasswordDialog(it) } }
    private val organizePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { showOrganizeDialog(it) } }
    private val editPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { launchPdfEditor(it) } }

    companion object {
        const val EXTRA_OPEN_TOOL = "open_tool"
        const val TOOL_MERGE = "merge"
        const val TOOL_SPLIT = "split"
        const val TOOL_COMPRESS = "compress"
        const val TOOL_WATERMARK = "watermark"
        const val TOOL_PASSWORD = "password"
        const val TOOL_ORGANIZE = "organize"
        const val TOOL_EDIT = "edit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityPdfToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupListeners()
        handleDeepLink()
    }
    
    private fun handleDeepLink() {
        val tool = intent.getStringExtra(EXTRA_OPEN_TOOL)
        if (tool != null && UserPremiums.isPro) {
            binding.root.post {
                when (tool) {
                    TOOL_MERGE -> mergePicker.launch(arrayOf("application/pdf"))
                    TOOL_SPLIT -> splitPicker.launch(arrayOf("application/pdf"))
                    TOOL_COMPRESS -> compressPicker.launch(arrayOf("application/pdf"))
                    TOOL_WATERMARK -> watermarkPicker.launch(arrayOf("application/pdf"))
                    TOOL_PASSWORD -> passwordPicker.launch(arrayOf("application/pdf"))
                    TOOL_ORGANIZE -> organizePicker.launch(arrayOf("application/pdf"))
                    TOOL_EDIT -> editPicker.launch(arrayOf("application/pdf"))
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.cardMerge.setOnClickListener {
            if (UserPremiums.isPro) mergePicker.launch(arrayOf("application/pdf"))
            else Toast.makeText(this, "Pro Feature: Merging Locked", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardSplit.setOnClickListener {
             if (UserPremiums.isPro) splitPicker.launch(arrayOf("application/pdf"))
             else Toast.makeText(this, "Pro Feature: Splitting Locked", Toast.LENGTH_SHORT).show()
        }

        binding.cardCompress.setOnClickListener {
             if (UserPremiums.isPro) compressPicker.launch(arrayOf("application/pdf"))
             else Toast.makeText(this, "Pro Feature: Compression Locked", Toast.LENGTH_SHORT).show()
        }

        binding.cardWatermark.setOnClickListener {
             if (UserPremiums.isPro) watermarkPicker.launch(arrayOf("application/pdf"))
             else Toast.makeText(this, "Pro Feature: Watermarking Locked", Toast.LENGTH_SHORT).show()
        }

        binding.cardPassword.setOnClickListener {
             if (UserPremiums.isPro) passwordPicker.launch(arrayOf("application/pdf"))
             else Toast.makeText(this, "Pro Feature: Security Locked", Toast.LENGTH_SHORT).show()
        }

        binding.cardOrganize.setOnClickListener {
             if (UserPremiums.isPro) organizePicker.launch(arrayOf("application/pdf"))
             else Toast.makeText(this, "Pro Feature: Organization Locked", Toast.LENGTH_SHORT).show()
        }

        binding.cardEditPdf.setOnClickListener {
             if (UserPremiums.isPro) editPicker.launch(arrayOf("application/pdf"))
             else Toast.makeText(this, "Pro Feature: Editing Locked", Toast.LENGTH_SHORT).show()
        }
    }
    
    // --- DIALOGS & PROCESSING ---
    
    private fun showCompressDialog(uri: Uri) {
        val options = arrayOf("High Quality (Less Compression)", "Balanced", "Low Quality (Max Compression)")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Compress PDF")
            .setSingleChoiceItems(options, 1) { dialog, which ->
                dialog.dismiss()
                performCompress(uri, which)
            }
            .show()
    }
    
    private fun performCompress(uri: Uri, level: Int) {
         binding.progressBar.visibility = View.VISIBLE
         binding.tvProgressStatus.text = "Compressing..."
         
         CoroutineScope(Dispatchers.IO).launch {
             try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")!!
                val fileName = "Compressed_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)!!
                val tempOut = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                // Level: 0=High(80), 1=Balanced(50), 2=Low(20)
                val quality = when(level) { 0 -> 80; 1 -> 50; else -> 20 }
                
                PdfUtilityTools.compressPdf(cacheFile.absolutePath, tempOut.absolutePath, quality)
                
                contentResolver.openOutputStream(outputUri)?.use { tempOut.inputStream().copyTo(it) }
                ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                
                withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Compressed PDF Saved!", Toast.LENGTH_LONG).show() }
             } catch (e: Exception) {
                 withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
             } finally {
                 withContext(Dispatchers.Main) { binding.progressBar.visibility = View.GONE }
             }
         }
    }

    private fun showWatermarkDialog(uri: Uri) {
        val input = android.widget.EditText(this).apply { hint = "Enter Watermark Text" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Watermark")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) performWatermark(uri, text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performWatermark(uri: Uri, text: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Watermarking..."
        
        CoroutineScope(Dispatchers.IO).launch {
             try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")!!
                val fileName = "Watermarked_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)!!
                val tempOut = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                val config = PdfUtilityTools.WatermarkConfig(text = text)
                PdfUtilityTools.watermarkPdfAdvanced(cacheFile.absolutePath, tempOut.absolutePath, config)
                
                contentResolver.openOutputStream(outputUri)?.use { tempOut.inputStream().copyTo(it) }
                ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                
                withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Watermarked PDF Saved!", Toast.LENGTH_LONG).show() }
             } catch (e: Exception) {
                 withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
             } finally {
                 withContext(Dispatchers.Main) { binding.progressBar.visibility = View.GONE }
             }
         }
    }

    private fun showPasswordDialog(uri: Uri) {
        val input = android.widget.EditText(this).apply { 
            hint = "Enter Password" 
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Protect PDF")
            .setView(input)
            .setPositiveButton("Encrypt") { _, _ ->
                val pass = input.text.toString()
                if (pass.isNotEmpty()) performEncrypt(uri, pass)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performEncrypt(uri: Uri, pass: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Encrypting..."
        
        CoroutineScope(Dispatchers.IO).launch {
             try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")!!
                val fileName = "Protected_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)!!
                val tempOut = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                PdfUtilityTools.encryptPdf(cacheFile.absolutePath, tempOut.absolutePath, pass, pass)
                
                contentResolver.openOutputStream(outputUri)?.use { tempOut.inputStream().copyTo(it) }
                ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                
                withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Encrypted PDF Saved!", Toast.LENGTH_LONG).show() }
             } catch (e: Exception) {
                 withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
             } finally {
                 withContext(Dispatchers.Main) { binding.progressBar.visibility = View.GONE }
             }
         }
    }

    private fun showOrganizeDialog(uri: Uri) {
         val input = android.widget.EditText(this).apply { 
             hint = "New Order (e.g., 2,1,3)"
             inputType = android.text.InputType.TYPE_CLASS_TEXT
         }
         androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reorder Pages")
            .setMessage("Enter the new page sequence (comma-separated).\nOnly include pages you want to keep.")
            .setView(input)
            .setPositiveButton("Reorder") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    try {
                        val pages = text.split(",").map { it.trim().toInt() }
                        performReorder(uri, pages)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid Format", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performReorder(uri: Uri, newOrder: List<Int>) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Reordering..."
        
        CoroutineScope(Dispatchers.IO).launch {
             try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")!!
                val fileName = "Reordered_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)!!
                val tempOut = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                PdfUtilityTools.reorderPdf(cacheFile.absolutePath, tempOut.absolutePath, newOrder)
                
                contentResolver.openOutputStream(outputUri)?.use { tempOut.inputStream().copyTo(it) }
                ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                
                withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Reordered PDF Saved!", Toast.LENGTH_LONG).show() }
             } catch (e: Exception) {
                 withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
             } finally {
                 withContext(Dispatchers.Main) { binding.progressBar.visibility = View.GONE }
             }
         }
    }

    // MERGE LOGIC
    private fun performMerge(uris: List<Uri>) {
        if (uris.size < 2) {
            Toast.makeText(this, "Select at least 2 PDFs", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Merging..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Copy all uris to cache
                val cacheFiles = uris.mapNotNull { uri ->
                    ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")
                }
                
                if (cacheFiles.size < uris.size) {
                    throw Exception("Could not access some files")
                }
                
                // 2. Prepare Output Uri
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "Merged_$timestamp.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)
                    ?: throw Exception("Could not create output file")
                    
                // 3. Perform Merge (Using OpenPDF via PdfUtilityTools)
                val tempOutputFile = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                val result = PdfUtilityTools.mergePdfs(
                    cacheFiles.map { it.absolutePath },
                    tempOutputFile.absolutePath
                )
                
                if (result.isSuccess) {
                    contentResolver.openOutputStream(outputUri)?.use { out ->
                        tempOutputFile.inputStream().copyTo(out)
                    }
                    ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PdfToolsActivity, "Merged PDF Saved in Documents!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    throw Exception("Merge failed in engine")
                }
                
                // Cleanup
                cacheFiles.forEach { it.delete() }
                tempOutputFile.delete()
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PdfToolsActivity, "Merge Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    // SPLIT LOGIC
    private fun performSplit(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Splitting..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")
                    ?: throw Exception("Could not cache file")
                    
                val tempDir = File(cacheDir, "split_temp").apply { mkdirs() }
                
                val result = PdfUtilityTools.splitPdf(
                    cacheFile.absolutePath,
                    tempDir.absolutePath
                )
                
                if (result.isSuccess) {
                    val files = result.getOrNull() ?: emptyList()
                    var savedCount = 0
                    files.forEachIndexed { index, file ->
                        val fileName = "Split_${index + 1}_${file.name}"
                        val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)
                        if (outputUri != null) {
                            contentResolver.openOutputStream(outputUri)?.use { out ->
                                file.inputStream().copyTo(out)
                            }
                            ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                            savedCount++
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PdfToolsActivity, "Split $savedCount pages to Documents!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    throw Exception("Split failed in engine")
                }
                
                cacheFile.delete()
                tempDir.deleteRecursively()
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PdfToolsActivity, "Split Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun launchPdfEditor(uri: Uri) {
        val intent = Intent(this, PdfEditorActivity::class.java).apply {
            data = uri
        }
        startActivity(intent)
    }
}
