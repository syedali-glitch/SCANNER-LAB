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
class MainActivity : AppCompatActivity() {
    
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
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permission granted
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
        setupGestures()
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
        // --- Main Cards & Shortcuts ---

        // Scanner Hub (Large Card) -> defaults to Doc Scanner
        binding.cardScannerHub.setOnClickListener {
            it.performHapticFeedback()
            checkCameraPermissionAndLaunch {
                startActivity(Intent(this, DocumentScannerActivity::class.java))
            }
        }

        // Shortcut: QR Code
        binding.btnShortcutQr.setOnClickListener {
            it.performHapticFeedback()
            checkCameraPermissionAndLaunch {
                startActivity(Intent(this, QRScannerActivity::class.java))
            }
        }

        // Shortcut: Document
        binding.btnShortcutDoc.setOnClickListener {
            it.performHapticFeedback()
            checkCameraPermissionAndLaunch {
                startActivity(Intent(this, DocumentScannerActivity::class.java))
            }
        }

        // Shortcut: Barcode (re-using QR activity for now)
        binding.btnShortcutBarcode.setOnClickListener {
            it.performHapticFeedback()
            checkCameraPermissionAndLaunch {
                startActivity(Intent(this, QRScannerActivity::class.java))
            }
        }
        
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

        // PDF Tools Card
        binding.cardPdfTools.setOnClickListener {
            it.performHapticFeedback()
            if (com.scanner.lab.utils.UserPremiums.checkOrShowUpsell(this)) {
                startActivity(Intent(this, PdfToolsActivity::class.java))
            }
        }
        
        setupBottomNav()
    }
    
    private fun setupBottomNav() {
        // Since we are using a custom LinearLayout for bottom nav, we bind by child index or ID if we assigned them
        // For simplicity, let's assume we can get them by child index from the parent LinearLayout
        // But better to use IDs if we can. In xml we didn't assign IDs to the bottom nav items yet, 
        // let's just leave it for now or use the "Home" one which effectively does nothing (already here)
        // We will need to update XML to give IDs to bottom nav items for proper handling.
    }

    private fun setupGestures() {
        // Add swipe gestures to cards
        GestureHandler(this, binding.cardScannerHub)
            .setOnSwipeRight {
                // Swipe on scanner hub -> open history? or just camera
                 checkCameraPermissionAndLaunch {
                    startActivity(Intent(this, DocumentScannerActivity::class.java))
                }
            }
            .setOnDoubleTap {
                binding.cardScannerHub.animatePulse()
            }
        
        // Keep gesture logic simple for now
    }
    
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
}
