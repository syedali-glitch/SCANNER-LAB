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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        
        // Load banner ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        
        setupUI()
        setupGestures()
        requestPermissionsIfNeeded()
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
        
        // File Converter Button
        binding.btnConverter.setOnClickListener {
            it.performHapticFeedback()
            // Open file picker for conversion
            Toast.makeText(this, "File Converter - Coming in next update", Toast.LENGTH_SHORT).show()
        }
        
        // PDF Tools Button
        binding.btnPdfTools.setOnClickListener {
            it.performHapticFeedback()
            // Open PDF tools
            Toast.makeText(this, "PDF Tools - Coming in next update", Toast.LENGTH_SHORT).show()
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
        }
        
        if (permissions.isNotEmpty()) {
            // Request permissions
        }
    }
    
    private fun android.view.View.performHapticFeedback() {
        this.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
