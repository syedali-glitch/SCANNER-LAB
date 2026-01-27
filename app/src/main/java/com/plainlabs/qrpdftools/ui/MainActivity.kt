import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.ads.AdManager
import com.plainlabs.qrpdftools.data.local.entity.ScanType
import com.plainlabs.qrpdftools.databinding.ActivityMainBinding
import com.plainlabs.qrpdftools.ui.favorites.FavoritesBottomSheet
import com.plainlabs.qrpdftools.ui.history.HistoryBottomSheet
import com.plainlabs.qrpdftools.ui.main.MainViewModel
import com.plainlabs.qrpdftools.ui.scanner.BarcodeScannerAnalyzer
import com.plainlabs.qrpdftools.ui.settings.SettingsBottomSheet
import com.plainlabs.qrpdftools.util.ErrorHandler
import com.plainlabs.qrpdftools.util.ScreenUtil
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var adManager: AdManager
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application)
    }
    
    private var camera: Camera? = null
    private var scanningEnabled = true
    
    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                this,
                R.string.camera_permission_required,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Enable high refresh rate for smooth animations
        ErrorHandler.safe("MainActivity", Unit, "Refresh rate optimization failed") {
            ScreenUtil.enableHighRefreshRate(this)
        }
        
        // Log screen info for debugging
        logScreenInfo()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        adManager = AdManager(this)
        
        setupUI()
        checkCameraPermission()
        observeViewModel()
        
        // Start FAB pulse animation
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.fab_pulse)
        binding.fab.startAnimation(pulseAnim)
        
        // Preload ads
        adManager.preloadInterstitialAd()
        adManager.preloadRewardedAd()
    }
    
    private fun logScreenInfo() {
        val screenInfo = ScreenUtil.getScreenInfo(this)
        Log.d("MainActivity", """
            Screen Info:
            - Resolution: ${screenInfo.resolution}
            - Size: ${screenInfo.widthDp}x${screenInfo.heightDp} dp
            - Density: ${screenInfo.densityBucket}
            - Screen: ${String.format("%.2f", screenInfo.sizeInches)}" ${screenInfo.sizeCategory}
            - Refresh: ${screenInfo.refreshRate}Hz
            - Orientation: ${if (screenInfo.isLandscape) "Landscape" else "Portrait"}
        """.trimIndent())
    }
    
    private fun setupUI() {
        binding.btnHistory.setOnClickListener {
            showHistory()
        }
        
        binding.btnFavorites.setOnClickListener {
            showFavorites()
        }
        
        binding.btnSettings.setOnClickListener {
            showSettings()
        }
        
        binding.fab.setOnClickListener {
            // Toggle between scan modes (could add PDF mode here)
            scanningEnabled = !scanningEnabled
            if (scanningEnabled) {
                binding.scanStatus.text = getString(R.string.point_camera)
            } else {
                binding.scanStatus.text = getString(R.string.scanning)
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.adsRemovedFlow.collect { adsRemoved ->
                if (adsRemoved) {
                    binding.adView.visibility = android.view.View.GONE
                } else {
                    adManager.loadBannerAd(binding.adView)
                }
            }
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startCamera() {
        ErrorHandler.safe("MainActivity", Unit, ErrorHandler.Messages.CAMERA_ERROR) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    // Preview
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(binding.previewView.surfaceProvider)
                        }
                    
                    // Image analysis for barcode scanning
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                BarcodeScannerAnalyzer { barcodes ->
                                    processBarcodes(barcodes)
                                }
                            )
                        }
                    
                    // Select back camera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    ErrorHandler.handleError("MainActivity", e, ErrorHandler.Messages.CAMERA_ERROR)
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }
    
    private fun processBarcodes(barcodes: List<Barcode>) {
        ErrorHandler.safe("MainActivity", Unit, ErrorHandler.Messages.SCAN_ERROR) {
            if (!scanningEnabled || barcodes.isEmpty()) return@safe
            
            val barcode = barcodes.first()
            val rawValue = barcode.rawValue ?: return@safe
            
            // Disable scanning temporarily to prevent duplicate scans
            scanningEnabled = false
            
            runOnUiThread {
                try {
                    // Show success animation
                    val successAnim = AnimationUtils.loadAnimation(this, R.anim.scan_success)
                    binding.fab.startAnimation(successAnim)
                    
                    // Update status
                    binding.scanStatus.text = getString(R.string.scan_success)
                    
                    // Determine scan type
                    val scanType = when (barcode.valueType) {
                        Barcode.TYPE_URL,
                        Barcode.TYPE_TEXT,
                        Barcode.TYPE_WIFI,
                        Barcode.TYPE_CONTACT_INFO -> ScanType.QR_CODE
                        else -> ScanType.BARCODE
                    }
                    
                    // Save to database
                    viewModel.saveScan(
                        content = rawValue,
                        type = scanType,
                        format = barcode.format.toString()
                    )
                    
                    // Show interstitial ad (first scan)
                    lifecycleScope.launch {
                        if (viewModel.isFirstScan()) {
                            adManager.showInterstitialAd(this@MainActivity) {
                                // Re-enable scanning after ad
                                scanningEnabled = true
                                binding.scanStatus.text = getString(R.string.point_camera)
                            }
                        } else {
                            // Re-enable scanning
                            android.os.Handler(mainLooper).postDelayed({
                                scanningEnabled = true
                                binding.scanStatus.text = getString(R.string.point_camera)
                            }, 2000)
                        }
                    }
                    
                    Toast.makeText(this, "Scan saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    ErrorHandler.handleError("MainActivity", e, "Scan processing failed")
                    scanningEnabled = true
                }
            }
        }
    }
    
    private fun showHistory() {
        HistoryBottomSheet().show(supportFragmentManager, "history")
    }
    
    private fun showFavorites() {
        FavoritesBottomSheet().show(supportFragmentManager, "favorites")
    }
    
    private fun showSettings() {
        SettingsBottomSheet(
            onAdsRemoved = {
                binding.adView.visibility = android.view.View.GONE
            }
        ).show(supportFragmentManager, "settings")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        adManager.destroy()
    }
}
