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
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
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
        // QR Scanner Button
        binding.btnQrScanner.setOnClickListener {
            it.performHapticFeedback()
            checkCameraPermissionAndLaunch {
                startActivity(Intent(this, QRScannerActivity::class.java))
            }
        }
        
        // Document Scanner Button
        binding.btnDocScanner.setOnClickListener {
            it.performHapticFeedback()
            checkCameraPermissionAndLaunch {
                startActivity(Intent(this, DocumentScannerActivity::class.java))
            }
        }
        
        // File Converter Button (Gated Feature)
        binding.btnConverter.setOnClickListener {
            it.performHapticFeedback()
            
            // "Monetization Gate": Check Pro Status before allowing "Excel Export" (simulated)
            if (com.scanner.lab.utils.UserPremiums.checkOrShowUpsell(this)) {
                startActivity(Intent(this, ConverterActivity::class.java))
            }
        }
        
        // PDF Tools Button (Gated Feature)
        binding.btnPdfTools.setOnClickListener {
            it.performHapticFeedback()
            
            // Allow access (or gate if needed, but defaults to True now)
            if (com.scanner.lab.utils.UserPremiums.checkOrShowUpsell(this)) {
                startActivity(Intent(this, PdfToolsActivity::class.java))
            }
        }
    }
    
    private fun setupGestures() {
        // Add swipe gestures to cards
        GestureHandler(this, binding.cardQrScanner)
            .setOnSwipeRight {
                binding.btnQrScanner.performClick()
            }
            .setOnDoubleTap {
                binding.btnQrScanner.animatePulse()
            }
        
        GestureHandler(this, binding.cardDocScanner)
            .setOnSwipeRight {
                binding.btnDocScanner.performClick()
            }
            .setOnDoubleTap {
                binding.btnDocScanner.animatePulse()
            }
        
        GestureHandler(this, binding.cardConverter)
            .setOnSwipeRight {
                binding.btnConverter.performClick()
            }
            .setOnDoubleTap {
                binding.btnConverter.animatePulse()
            }
        
        GestureHandler(this, binding.cardPdfTools)
            .setOnSwipeRight {
                binding.btnPdfTools.performClick()
            }
            .setOnDoubleTap {
                binding.btnPdfTools.animatePulse()
            }
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
}
