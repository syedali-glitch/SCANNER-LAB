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

    companion object {
        const val EXTRA_OPEN_TOOL = "open_tool"
        const val TOOL_MERGE = "merge"
        const val TOOL_SPLIT = "split"
        const val TOOL_COMPRESS = "compress"
        const val TOOL_WATERMARK = "watermark"
        const val TOOL_PASSWORD = "password"
        const val TOOL_ORGANIZE = "organize"
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
             hint = "Page numbers to remove (e.g., 1,3,5)"
             inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
         }
         androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Pages")
            .setMessage("Enter comma-separated page numbers to DELETE.")
            .setView(input)
            .setPositiveButton("Remove") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    try {
                        val pages = text.split(",").map { it.trim().toInt() }
                        performOrganize(uri, pages)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid Format", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performOrganize(uri: Uri, pagesToRemove: List<Int>) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressStatus.text = "Organizing..."
        
        CoroutineScope(Dispatchers.IO).launch {
             try {
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")!!
                val fileName = "Organized_${System.currentTimeMillis()}.pdf"
                val outputUri = ScopedStorageHelper.createDocumentUri(this@PdfToolsActivity, fileName)!!
                val tempOut = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                PdfUtilityTools.removePages(cacheFile.absolutePath, tempOut.absolutePath, pagesToRemove)
                
                contentResolver.openOutputStream(outputUri)?.use { tempOut.inputStream().copyTo(it) }
                ScopedStorageHelper.finalizeFile(this@PdfToolsActivity, outputUri)
                
                withContext(Dispatchers.Main) { Toast.makeText(this@PdfToolsActivity, "Organized PDF Saved!", Toast.LENGTH_LONG).show() }
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
                // PdfUtilityTools.mergePdfs takes paths and returns File.
                // We need to bridge this.
                // Create a temp output file first
                val tempOutputFile = ScopedStorageHelper.createCacheFile(this@PdfToolsActivity, "pdf")
                
                val result = PdfUtilityTools.mergePdfs(
                    cacheFiles.map { it.absolutePath },
                    tempOutputFile.absolutePath
                )
                
                if (result.isSuccess) {
                    // 4. Write success file to target Uri
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
                // 1. Copy source to cache
                val cacheFile = ScopedStorageHelper.copyUriToCache(this@PdfToolsActivity, uri, "pdf")
                    ?: throw Exception("Could not cache file")
                    
                // 2. Create Output Dir (Use a temp dir for splitting results)
                val tempDir = File(cacheDir, "split_temp").apply { mkdirs() }
                
                // 3. Perform Split (Using OpenPDF)
                // Returns list of files
                val result = PdfUtilityTools.splitPdf(
                    cacheFile.absolutePath,
                    tempDir.absolutePath
                )
                
                if (result.isSuccess) {
                    val files = result.getOrNull() ?: emptyList()
                    
                    // 4. Save each file to Documents
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
                
                // Cleanup
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
}
