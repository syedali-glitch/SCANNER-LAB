package com.scanner.lab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.scanner.lab.databinding.ActivityToolsBinding
import com.scanner.lab.databinding.ItemToolCardBinding

/**
 * Stage 2: Tools & Engines
 * Central hub for all 17 scanning utilities.
 */
class ToolsActivity : BaseActivity() {

    private lateinit var binding: ActivityToolsBinding

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
        // Pass the ROOT view of the included layout
        setupTool(binding.toolAutoEdge.root, "Auto-Edge", R.drawable.ic_scan_doc, true) {
            startActivity(Intent(this, DocumentScannerActivity::class.java))
        }
        setupTool(binding.toolPerspective.root, "Perspective", R.drawable.ic_scan_doc, true) {
            startActivity(Intent(this, DocumentScannerActivity::class.java))
        }
        setupTool(binding.toolMagicFilter.root, "Magic Filter", R.drawable.ic_scan_doc, true) {
            showEngineInfo("Magic Color Filter", "Engine: Adaptive Thresholding + CLAHE.\n\nStatus: Ready for Integration.")
        }
        setupTool(binding.toolShadowRemoval.root, "Shadow Remover", R.drawable.ic_scan_doc, false) {
            showEngineInfo("Shadow & Glare Removal", "Engine: Illumination Correction Map.\n\nStatus: Placeholder for v1.0.1.")
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
        setupTool(binding.toolHandwriting.root, "Handwriting", R.drawable.ic_file_viewer, false) {
            showEngineInfo("Handwriting OCR", "Engine: ML Kit Digital Ink Recognition.\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(binding.toolSearchablePdf.root, "Searchable PDF", R.drawable.ic_pdf_tool, false) {
            showEngineInfo("Searchable PDF", "Engine: PDF/A Overlay (Text Layer).\n\nStatus: Placeholder for v1.0.1.")
        }

        // --- Section 3: Specialized Modes ---
        setupTool(binding.toolIdCard.root, "ID Card Mode", R.drawable.ic_scan_doc, false) {
            showEngineInfo("ID Card Mode", "Engine: Canvas Composition (Front + Back).\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(binding.toolPassport.root, "Passport (MRZ)", R.drawable.ic_scan_doc, false) {
            showEngineInfo("Passport Reader", "Engine: MRZ Parser (ICAO 9303).\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(binding.toolBookMode.root, "Book Mode", R.drawable.ic_scan_doc, false) {
             showEngineInfo("Book Curve Correction", "Engine: 3D Cylindrical Projection.\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(binding.toolFingerRemoval.root, "Finger Removal", R.drawable.ic_scan_doc, false) {
             showEngineInfo("Finger Removal", "Engine: Inpainting (Navier-Stokes).\n\nStatus: Placeholder for v1.0.1.")
        }

        // --- Section 4: Security ---
        setupTool(binding.toolEncryption.root, "PDF Encrypt", R.drawable.ic_pdf_tool, true) {
            startActivity(Intent(this, PdfToolsActivity::class.java))
        }
        setupTool(binding.toolSignature.root, "E-Signature", R.drawable.ic_file_viewer, false) {
            showEngineInfo("E-Signature", "Engine: Bezier Curve Vector Path.\n\nStatus: Placeholder for v1.0.1.")
        }
        setupTool(binding.toolPrivateSpace.root, "Private Space", R.drawable.ic_settings, false) {
             showEngineInfo("Private Space", "Engine: AES-256 Storage & Biometrics.\n\nStatus: Placeholder for v1.0.1.")
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
