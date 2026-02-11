package com.scanner.lab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.scanner.lab.databinding.ActivityToolsBinding
import com.scanner.lab.databinding.ItemToolCardBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stage 2: Tools & Engines
 * Central hub for all 17 scanning utilities.
 */
class ToolsActivity : BaseActivity() {

    private lateinit var binding: ActivityToolsBinding

    // ... (pickers)

    // ... (onCreate)

    // ... (setupUI)

    // ... (setupBottomNav)


    

    // IMAGE PICKERS for Tools
    private val shadowPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> 
        uri?.let { showImageProcessingDialog(it, com.scanner.lab.utils.ImageProcessor.FilterMode.SHADOW_REMOVER, "Shadow Removal") } 
    }
    
    private val magicPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> 
        uri?.let { showImageProcessingDialog(it, com.scanner.lab.utils.ImageProcessor.FilterMode.MAGIC_V2, "Magic Filter") } 
    }
    
    private val securityPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> 
        uri?.let { showImageProcessingDialog(it, com.scanner.lab.utils.ImageProcessor.FilterMode.SECURITY_PATTERN, "Anti-Counterfeit") } 
    }
    
    private val annotationPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> 
        uri?.let { 
             val intent = Intent(this, com.scanner.lab.tools.AnnotationActivity::class.java)
             intent.data = it
             startActivity(intent)
        } 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupBottomNav()
    }

    private fun setupUI() {
        // --- Section 1: Core Scanning ---
        setupTool(binding.toolAutoEdge.root, "Auto-Edge", R.drawable.ic_scan_doc, true) {
            startActivity(Intent(this, DocumentScannerActivity::class.java))
        }
        setupTool(binding.toolPerspective.root, "Perspective", R.drawable.ic_scan_doc, true) {
            startActivity(Intent(this, DocumentScannerActivity::class.java))
        }
        setupTool(binding.toolMagicFilter.root, "Magic Filter", R.drawable.ic_scan_doc, true) {
            magicPicker.launch(arrayOf("image/*"))
        }
        setupTool(binding.toolShadowRemoval.root, "Shadow Remover", R.drawable.ic_scan_doc, true) {
            shadowPicker.launch(arrayOf("image/*"))
        }

        // --- Section 2: Intelligent OCR ---
        setupTool(binding.toolOcr.root, "Offline OCR", R.drawable.ic_file_viewer, true) {
            startActivity(Intent(this, ConverterActivity::class.java))
        }
        setupTool(binding.toolExcel.root, "Scan to Excel", R.drawable.ic_upload, true) {
            startActivity(Intent(this, ConverterActivity::class.java))
        }
        setupTool(binding.toolWord.root, "Scan to Word", R.drawable.ic_upload, true) {
            startActivity(Intent(this, ConverterActivity::class.java))
        }
        setupTool(binding.toolPpt.root, "Scan to PPT", R.drawable.ic_upload, false) {
            showEngineInfo("Scan to PowerPoint", "Engine: Layout Analysis & XML Generation.\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(binding.toolHandwriting.root, "Handwriting", R.drawable.ic_file_viewer, true) {
            startActivity(Intent(this, com.scanner.lab.tools.HandwritingActivity::class.java))
        }
        setupTool(binding.toolSearchablePdf.root, "Searchable PDF", R.drawable.ic_pdf_tool, true) {
             showEngineInfo("Searchable PDF", "Engine: PDF/A Overlay (Text Layer).\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(findViewById(R.id.toolTranslator), "Translator", R.drawable.ic_file_viewer, true) {
             startActivity(Intent(this, com.scanner.lab.tools.TranslatorActivity::class.java))
        }

        // --- Section 3: PDF Tools & Utilities ---
        setupTool(binding.toolPdfMerge.root, "Merge PDF", R.drawable.ic_pdf_tool, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_MERGE)
             startActivity(intent)
        }
        setupTool(binding.toolPdfSplit.root, "Split PDF", R.drawable.ic_pdf_tool, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_SPLIT)
             startActivity(intent)
        }
        setupTool(binding.toolPdfCompress.root, "Compress PDF", R.drawable.ic_pdf_tool, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_COMPRESS)
             startActivity(intent)
        }
        setupTool(binding.toolPdfWatermark.root, "Watermark", R.drawable.ic_pdf_tool, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_WATERMARK)
             startActivity(intent)
        }
        setupTool(binding.toolPdfOrganize.root, "Organize Pages", R.drawable.ic_pdf_tool, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_ORGANIZE)
             startActivity(intent)
        }
        setupTool(binding.toolPdfPassword.root, "PDF Password", R.drawable.ic_lock, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_PASSWORD)
             startActivity(intent)
        }

        // --- Section 4: Specialized Modes ---
        setupTool(binding.toolIdCard.root, "ID Card Mode", R.drawable.ic_scan_doc, true) {
            val intent = Intent(this, DocumentScannerActivity::class.java)
            intent.putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.ID_CARD.ordinal)
            startActivity(intent)
        }
        setupTool(binding.toolPassport.root, "Passport (MRZ)", R.drawable.ic_scan_doc, true) {
            val intent = Intent(this, DocumentScannerActivity::class.java)
            intent.putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.PASSPORT.ordinal)
            startActivity(intent)
        }
        setupTool(binding.toolBookMode.root, "Book Mode", R.drawable.ic_scan_doc, true) {
             val intent = Intent(this, DocumentScannerActivity::class.java)
             intent.putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.BOOK.ordinal)
             startActivity(intent)
        }
        setupTool(binding.toolFingerRemoval.root, "Finger Removal", R.drawable.ic_scan_doc, true) {
             startActivity(Intent(this, com.scanner.lab.tools.FingerRemovalActivity::class.java))
        }
        setupTool(findViewById(R.id.toolAnnotation), "Annotation", R.drawable.ic_file_viewer, true) {
             annotationPicker.launch(arrayOf("image/*"))
        }

        // --- Section 5: Security & Management ---
        // Encryption moved to PDF Tools section
        setupTool(binding.toolSignature.root, "E-Signature", R.drawable.ic_file_viewer, true) {
            startActivity(Intent(this, com.scanner.lab.tools.SignatureActivity::class.java))
        }
        setupTool(binding.toolPrivateSpace.root, "Private Space", R.drawable.ic_settings, true) {
             startActivity(Intent(this, com.scanner.lab.tools.PrivateSpaceActivity::class.java))
        }
        setupTool(binding.toolAntiCounterfeit.root, "Anti-Counterfeit", R.drawable.ic_lock, true) {
             securityPicker.launch(arrayOf("image/*"))
        }
    }
    
    private fun showImageProcessingDialog(uri: android.net.Uri, filterMode: com.scanner.lab.utils.ImageProcessor.FilterMode, title: String) {

        // Ensure dialog layout exists or create it? 
        // We might not have 'dialog_image_preview'. Let's check or create a simple view programmatically if needed.
        // For safety, I'll allow the agent to create the layout if missing, but let's assume I need to create it.
        // I will use a simple ImageView construction here to avoid crashing if layout is missing.
        
        val context = this
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = android.view.Gravity.CENTER
        }
        val imageView = android.widget.ImageView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(500, 700)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        val progress = android.widget.ProgressBar(context).apply {
            visibility = View.VISIBLE
        }
        layout.addView(imageView)
        layout.addView(progress)
        
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Save To Gallery") { _, _ ->
                // Save logic (the bitmap is in tag or we re-process)
                val processed = imageView.tag as? android.graphics.Bitmap
                if (processed != null) {
                    saveProcessedImage(processed, title)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.show()
        
        // Async Process
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheFile = com.scanner.lab.utils.ScopedStorageHelper.copyUriToCache(context, uri, "jpg")
                val original = android.graphics.BitmapFactory.decodeFile(cacheFile?.absolutePath)
                if (original != null) {
                    val processed = com.scanner.lab.utils.ImageProcessor.applyFilter(original, filterMode)
                    
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(processed)
                        imageView.tag = processed
                        progress.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
    }
    
    private fun saveProcessedImage(bitmap: android.graphics.Bitmap, toolName: String) {
         lifecycleScope.launch(Dispatchers.IO) {
             val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
             val fileName = "${toolName.replace(" ", "")}_$timestamp.jpg"
             val uri = com.scanner.lab.utils.ScopedStorageHelper.createDocumentUri(this@ToolsActivity, fileName, "image/jpeg")
             
             if (uri != null) {
                 contentResolver.openOutputStream(uri)?.use { out ->
                     bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                 }
                 com.scanner.lab.utils.ScopedStorageHelper.finalizeFile(this@ToolsActivity, uri)
                 
                 withContext(Dispatchers.Main) {
                     Toast.makeText(this@ToolsActivity, "Saved to Documents!", Toast.LENGTH_LONG).show()
                 }
             }
         }
    }

    private fun setupTool(view: View, title: String, iconRes: Int, isReady: Boolean, onClick: () -> Unit) {
        val itemBinding = ItemToolCardBinding.bind(view)
        
        itemBinding.tvToolName.text = title
        itemBinding.ivToolIcon.setImageResource(iconRes)
        
        // Optional: Dim icon if not ready
        if (!isReady) {
            itemBinding.ivToolIcon.alpha = 0.5f
            itemBinding.tvToolName.alpha = 0.7f
        }

        itemBinding.root.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }
    }

    private fun showEngineInfo(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupBottomNav() {
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }
        
        binding.navHistory.setOnClickListener {
             startActivity(Intent(this, FileViewerActivity::class.java))
             @Suppress("DEPRECATION")
             overridePendingTransition(0, 0)
             finish()
        }

        // Tools is active (no op)

        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
