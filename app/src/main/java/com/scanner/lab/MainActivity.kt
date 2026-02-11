package com.scanner.lab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.scanner.lab.databinding.ActivityMainBinding
import com.scanner.lab.ui.GestureHandler

/**
 * Main activity with premium glassmorphism UI
 */
class MainActivity : BaseActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch activity
        } else {
            Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show()
        }
    }
    
    private val recentAdapter = com.scanner.lab.ui.RecentAdapter { file -> openRecentFile(file) }
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permission granted
            loadRecentScans()
        } else {
            Toast.makeText(this, R.string.error_storage_permission, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge Polish
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Init Monetization
        com.scanner.lab.utils.UserPremiums.init(this)
        
        // Apply Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        
        // Load banner ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        
        setupUI()
        // setupGestures() // Removed as cardScannerHub is gone
        requestPermissionsIfNeeded()
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("PlainLabs Scanner")
            .setMessage("Version 1.0\n\nPatent Pending.\n\n© 2024 PlainLabs Inc.\nAll Rights Reserved.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun setupUI() {
        // --- Recent Scans ---
        binding.rvRecentScans.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecentScans.adapter = recentAdapter
        
        // --- Scanner Hub (Top) ---
        // 1. QR Code
        binding.root.findViewById<android.view.View>(R.id.btnHubQr)?.setOnClickListener {
             it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                val intent = Intent(this, QRScannerActivity::class.java)
                intent.putExtra("SCAN_MODE", "QR")
                startActivity(intent)
            }
        }

        // 2. Barcode
        binding.root.findViewById<android.view.View>(R.id.btnHubBarcode)?.setOnClickListener {
             it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                val intent = Intent(this, QRScannerActivity::class.java)
                intent.putExtra("SCAN_MODE", "BARCODE")
                startActivity(intent)
            }
        }

        // 3. Document
        binding.root.findViewById<android.view.View>(R.id.btnHubDoc)?.setOnClickListener {
             it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                val intent = Intent(this, DocumentScannerActivity::class.java)
                intent.putExtra("is_aio_mode", true)
                startActivity(intent)
            }
        }

        // --- Quick Tools ---
        // 1. Doc Scan
        binding.btnQuickDoc.setOnClickListener {
            it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                startActivity(Intent(this, DocumentScannerActivity::class.java))
            }
        }

        // 2. QR Code
        binding.btnQuickQr.setOnClickListener {
            it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                val intent = Intent(this, QRScannerActivity::class.java)
                intent.putExtra("SCAN_MODE", "QR")
                startActivity(intent)
            }
        }

        // 3. ID Card
        binding.btnQuickId.setOnClickListener {
             it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                val targetIntent = Intent(this, DocumentScannerActivity::class.java).apply {
                    putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.ID_CARD.ordinal)
                }
                val intent = Intent(this, com.scanner.lab.ui.IntroActivity::class.java).apply {
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TITLE, getString(R.string.intro_title_id_card))
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_DESC, getString(R.string.intro_desc_id_card))
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_ICON, R.drawable.ic_tool_id_card)
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TARGET_INTENT, targetIntent)
                }
                startActivity(intent)
            }
        }

        // 4. Passport
        binding.btnQuickPassport.setOnClickListener {
             it.performHapticFeedback()
             checkCameraPermissionAndLaunch {
                val targetIntent = Intent(this, DocumentScannerActivity::class.java).apply {
                    putExtra(DocumentScannerActivity.EXTRA_SCAN_MODE, com.scanner.lab.ui.ScannerOverlayView.ScanMode.PASSPORT.ordinal)
                }
                val intent = Intent(this, com.scanner.lab.ui.IntroActivity::class.java).apply {
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TITLE, getString(R.string.intro_title_passport))
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_DESC, getString(R.string.intro_desc_passport))
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_ICON, R.drawable.ic_tool_passport)
                    putExtra(com.scanner.lab.ui.IntroActivity.EXTRA_TARGET_INTENT, targetIntent)
                }
                startActivity(intent)
            }
        }

        // 5. E-Sign
        binding.btnQuickSign.setOnClickListener {
             it.performHapticFeedback()
             startActivity(Intent(this, com.scanner.lab.tools.SignatureActivity::class.java))
        }

        // Main Cards
        
        // File Converter Card
        binding.cardConverter.setOnClickListener {
            it.performHapticFeedback()
            if (com.scanner.lab.utils.UserPremiums.checkOrShowUpsell(this)) {
                startActivity(Intent(this, ConverterActivity::class.java))
            }
        }
        
        // File Viewer Card
        binding.cardFileViewer.setOnClickListener {
            it.performHapticFeedback()
            startActivity(Intent(this, FileViewerActivity::class.java))
        }
        
        setupBottomNav()
    }
    
    private fun setupBottomNav() {
        binding.navHome.setOnClickListener {
            it.performHapticFeedback()
            it.animatePulse()
            // Already on home, scroll to top
            binding.nestedScrollView.smoothScrollTo(0, 0)
        }

        binding.navHistory.setOnClickListener {
            it.performHapticFeedback()
            it.animatePulse()
            // History opens the File Viewer
            startActivity(Intent(this, FileViewerActivity::class.java))
        }

        binding.navTools.setOnClickListener {
            it.performHapticFeedback()
            it.animatePulse()
            startActivity(Intent(this, ToolsActivity::class.java))
        }

        binding.navSettings.setOnClickListener {
            it.performHapticFeedback()
            it.animatePulse()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /*
    private fun setupGestures() {
        // Add swipe gestures to cards
        // Removed cardScannerHub
    }
    */
    
    private fun checkCameraPermissionAndLaunch(onGranted: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            storagePermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun android.view.View.performHapticFeedback() {
        this.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun android.view.View.animatePulse() {
        this.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(100)
            .withEndAction {
                this.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    override fun onResume() {
        super.onResume()
        loadRecentScans()
    }

    private fun loadRecentScans() {
        // Simple scan of Documents/PlainLabsScanner
        val files = mutableListOf<java.io.File>()
        val docDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        val scannerDir = java.io.File(docDir, "PlainLabsScanner")
        
        if (scannerDir.exists()) {
             scannerDir.listFiles()?.let { files.addAll(it) }
        }
        
        // Internal
        getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)?.listFiles()?.let { files.addAll(it) }
        
        // Sort DESC
        files.sortByDescending { it.lastModified() }
        
        // Take 3
        val recent = files.take(3)
        
        if (recent.isNotEmpty()) {
            binding.tvRecentHeader.visibility = android.view.View.VISIBLE
            binding.rvRecentScans.visibility = android.view.View.VISIBLE
            recentAdapter.submitList(recent)
        } else {
            binding.tvRecentHeader.visibility = android.view.View.GONE
            binding.rvRecentScans.visibility = android.view.View.GONE
        }
    }

    private fun openRecentFile(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show()
        }
    }
}
