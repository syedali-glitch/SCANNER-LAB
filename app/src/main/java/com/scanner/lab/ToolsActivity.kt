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
        // --- Section 1: Convert Files (Green) ---
        setupTool(binding.toolOcr.root, "Extract Text", "Image -> Text", R.drawable.ic_tool_ocr, R.color.ref_office_green, true) {
            startActivity(Intent(this, ConverterActivity::class.java))
        }
        setupTool(binding.toolExcel.root, "Scan to Excel", "Image -> XLS", R.drawable.ic_tool_excel, R.color.ref_office_green, true) {
            val intent = Intent(this, ConverterActivity::class.java)
            intent.putExtra(ConverterActivity.EXTRA_CONVERSION_MODE, ConverterActivity.MODE_IMG_TO_EXCEL)
            startActivity(intent)
        }
        setupTool(binding.toolWord.root, "Scan to Word", "Image -> DOCX", R.drawable.ic_tool_word, R.color.ref_office_green, true) {
            val intent = Intent(this, ConverterActivity::class.java)
            intent.putExtra(ConverterActivity.EXTRA_CONVERSION_MODE, ConverterActivity.MODE_IMG_TO_WORD)
            startActivity(intent)
        }
        // FIXED: PPT Wired
        setupTool(binding.toolPpt.root, "Scan to PPT", "Image -> PPT", R.drawable.ic_tool_ppt, R.color.ref_office_green, true) {
            val intent = Intent(this, ConverterActivity::class.java)
            intent.putExtra(ConverterActivity.EXTRA_CONVERSION_MODE, ConverterActivity.MODE_IMG_TO_PPT)
            startActivity(intent)
        }
        setupTool(binding.toolHandwriting.root, "Handwriting", "Read Scripts", R.drawable.ic_tool_ocr, R.color.ref_ai_purple, true) {
            startActivity(Intent(this, com.scanner.lab.tools.HandwritingActivity::class.java))
        }
        // FIXED: Renamed to PDF to Text and Wired
        setupTool(binding.toolSearchablePdf.root, "PDF to Text", "PDF -> TXT", R.drawable.ic_tool_ocr, R.color.ref_office_green, true) {
            val intent = Intent(this, ConverterActivity::class.java)
            intent.putExtra(ConverterActivity.EXTRA_CONVERSION_MODE, ConverterActivity.MODE_PDF_TO_TEXT)
            startActivity(intent)
        }
        // FIXED: Hide broken Translator
        val toolTranslator = findViewById<View>(R.id.toolTranslator)
        if (toolTranslator != null) {
            toolTranslator.visibility = View.GONE
        }

        // --- Section 2: PDF Tools & Utilities (Red) ---
        setupTool(binding.toolPdfMerge.root, "Merge PDF", "Combine Files", R.drawable.ic_tool_pdf_merge, R.color.ref_pdf_red, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_MERGE)
             startActivity(intent)
        }
        setupTool(binding.toolPdfSplit.root, "Split PDF", "Extract Pages", R.drawable.ic_tool_pdf_split, R.color.ref_pdf_red, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_SPLIT)
             startActivity(intent)
        }
        setupTool(binding.toolPdfCompress.root, "Compress PDF", "Reduce Size", R.drawable.ic_tool_pdf_compress, R.color.ref_pdf_red, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_COMPRESS)
             startActivity(intent)
        }
        setupTool(binding.toolPdfWatermark.root, "Watermark", "Add Stamp", R.drawable.ic_tool_pdf_watermark, R.color.ref_pdf_red, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_WATERMARK)
             startActivity(intent)
        }
        setupTool(binding.toolPdfOrganize.root, "Organize Pages", "Reorder/Delete", R.drawable.ic_tool_pdf_organize, R.color.ref_pdf_red, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_ORGANIZE)
             startActivity(intent)
        }
        setupTool(binding.toolPdfPassword.root, "PDF Password", "Lock/Unlock", R.drawable.ic_tool_pdf_password, R.color.ref_pdf_red, true) {
             val intent = Intent(this, PdfToolsActivity::class.java)
             intent.putExtra(PdfToolsActivity.EXTRA_OPEN_TOOL, PdfToolsActivity.TOOL_PASSWORD)
             startActivity(intent)
        }

        // --- Section 3: Specialized Modes (Amber) ---
        setupTool(binding.toolIdCard.root, "ID Card Mode", "Front & Back", R.drawable.ic_tool_id_card, R.color.ref_id_amber, true) {
            val targetIntent = Intent(this, DocumentScannerActivity::class.java).apply {
                putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.ID_CARD.ordinal)
            }
            val intent = Intent(this, com.scanner.lab.ui.IntroActivity::class.java).apply {
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TITLE, "ID Card Mode")
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_DESC, "Capture both sides of an ID card seamlessly. The app will automatically stitch them onto a single A4 page for easy printing.")
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_ICON, R.drawable.ic_tool_id_card)
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TARGET_INTENT, targetIntent)
            }
            startActivity(intent)
        }
        setupTool(binding.toolPassport.root, "Passport Mode", "Read MRZ Code", R.drawable.ic_tool_passport, R.color.ref_id_amber, true) {
            val targetIntent = Intent(this, DocumentScannerActivity::class.java).apply {
                 putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.PASSPORT.ordinal)
            }
            val intent = Intent(this, com.scanner.lab.ui.IntroActivity::class.java).apply {
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TITLE, "Passport Mode")
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_DESC, "Align the passport data page within the frame. The app uses ML Kit to automatically extract MRZ data.")
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_ICON, R.drawable.ic_tool_passport)
                putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TARGET_INTENT, targetIntent)
            }
            startActivity(intent)
        }
        setupTool(binding.toolBookMode.root, "Book Mode", "Dual Page Scan", R.drawable.ic_scan_doc, R.color.ref_id_amber, true) {
             val intent = Intent(this, DocumentScannerActivity::class.java)
             intent.putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.BOOK.ordinal)
             startActivity(intent)
        }
        // Finger Removal -> Use Magic Icon for now
        setupTool(binding.toolFingerRemoval.root, "Finger Removal", "Remove Fingers", R.drawable.ic_tool_magic, R.color.ref_ai_purple, true) {
             startActivity(Intent(this, com.scanner.lab.tools.FingerRemovalActivity::class.java))
        }
        // Annotation -> Use Signature/Pen icon?
        setupTool(findViewById(R.id.toolAnnotation), "Annotation", "Draw on PDF", R.drawable.ic_tool_signature, R.color.ref_ai_purple, true) {
             annotationPicker.launch(arrayOf("image/*"))
        }

        // --- Section 5: Security & Management (Amber) ---
        setupTool(binding.toolSignature.root, "E-Signature", "Sign Docs", R.drawable.ic_tool_signature, R.color.ref_id_amber, true) {
            startActivity(Intent(this, com.scanner.lab.tools.SignatureActivity::class.java))
        }
        setupTool(binding.toolPrivateSpace.root, "Private Space", "Secure Vault", R.drawable.ic_tool_private, R.color.ref_id_amber, true) {
             startActivity(Intent(this, com.scanner.lab.tools.PrivateSpaceActivity::class.java))
        }
        setupTool(binding.toolAntiCounterfeit.root, "Anti-Counterfeit", "Verify Docs", R.drawable.ic_tool_private, R.color.ref_id_amber, true) {
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

    private fun setupTool(view: View, title: String, desc: String, iconRes: Int, colorRes: Int, isReady: Boolean, onClick: () -> Unit) {
        val itemBinding = ItemToolCardBinding.bind(view)
        
        itemBinding.tvToolName.text = title
        itemBinding.tvToolDesc.text = desc
        itemBinding.ivToolIcon.setImageResource(iconRes)
        
        // Dynamic Tinting
        // 1. Tint the Background Circle
        val color = androidx.core.content.ContextCompat.getColor(this, colorRes)
        itemBinding.ivToolIcon.background.setTint(color)
        
        // 2. Icon stays white (defined in vector) or we force it:
        itemBinding.ivToolIcon.setColorFilter(android.graphics.Color.WHITE)
        
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
