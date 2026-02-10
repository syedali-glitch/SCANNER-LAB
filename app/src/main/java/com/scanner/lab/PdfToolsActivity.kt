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
    
    // File Picker for Single PDF (Split)
    private val splitPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { performSplit(it) }
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
    }
    
    private fun setupListeners() {
        binding.cardMerge.setOnClickListener {
            if (UserPremiums.isPro) {
                mergePicker.launch(arrayOf("application/pdf"))
            } else {
                Toast.makeText(this, "Pro Feature: Merging Locked", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.cardSplit.setOnClickListener {
            if (UserPremiums.isPro) {
                splitPicker.launch(arrayOf("application/pdf"))
            } else {
                Toast.makeText(this, "Pro Feature: Splitting Locked", Toast.LENGTH_SHORT).show()
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
