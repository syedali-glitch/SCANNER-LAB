package com.plainlabs.qrpdftools.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.ads.AdManager
import com.plainlabs.qrpdftools.databinding.ActivityMainBinding
import com.plainlabs.qrpdftools.ui.main.MainViewModel
import com.plainlabs.qrpdftools.util.CrashLogger
import androidx.activity.viewModels
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var adManager: AdManager
    
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        CrashLogger.log("MainActivity", "==================== STARTUP ====================")
        CrashLogger.log("MainActivity", "onCreate() started")
        
        try {
            CrashLogger.log("MainActivity", "Step 1: Inflating layout...")
            binding = ActivityMainBinding.inflate(layoutInflater)
            CrashLogger.log("MainActivity", "✓ Layout inflated")
            
            CrashLogger.log("MainActivity", "Step 2: Setting content view...")
            setContentView(binding.root)
            CrashLogger.log("MainActivity", "✓ Content view set")
            
            CrashLogger.log("MainActivity", "Step 3: Creating AdManager...")
            adManager = AdManager(this)
            CrashLogger.log("MainActivity", "✓ AdManager created")
            
            CrashLogger.log("MainActivity", "Step 4: Setting up navigation...")
            setupNavigation()
            CrashLogger.log("MainActivity", "✓ Navigation setup complete")
            
            CrashLogger.log("MainActivity", "Step 5: Observing ViewModel...")
            observeViewModel()
            CrashLogger.log("MainActivity", "✓ ViewModel observer set")
            
            CrashLogger.log("MainActivity", "Step 6: Preloading ads...")
            adManager.preloadInterstitialAd()
            adManager.preloadRewardedAd()
            CrashLogger.log("MainActivity", "✓ Ads preloading")
            
            CrashLogger.log("MainActivity", "==================== SUCCESS ====================")
            CrashLogger.log("MainActivity", "App started successfully!")
            CrashLogger.log("MainActivity", "Log file: ${CrashLogger.getCrashLogPath()}")
            
        } catch (e: Exception) {
            CrashLogger.logError("MainActivity", "CRITICAL CRASH in onCreate", e)
            
            // Try to show error to user
            try {
                android.widget.Toast.makeText(
                    this,
                    "App failed to start. Check: ${CrashLogger.getCrashLogPath()}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e2: Exception) {
                CrashLogger.logError("MainActivity", "Toast also failed", e2)
            }
        }
    }
    
    private fun setupNavigation() {
        try {
            CrashLogger.log("MainActivity", "Finding NavHostFragment...")
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            if (navHostFragment == null) {
                CrashLogger.logError("MainActivity", "NavHostFragment NOT FOUND!")
                return
            }
            CrashLogger.log("MainActivity", "✓ NavHostFragment found")
            
            navController = navHostFragment.navController
            CrashLogger.log("MainActivity", "✓ NavController obtained")
            
            binding.bottomNavigation.setupWithNavController(navController)
            CrashLogger.log("MainActivity", "✓ Bottom navigation connected")
            
        } catch (e: Exception) {
            CrashLogger.logError("MainActivity", "Navigation setup failed", e)
        }
    }
    
    private fun observeViewModel() {
        try {
            lifecycleScope.launch {
                viewModel.adsRemovedFlow.collect { adsRemoved ->
                    if (adsRemoved) {
                        binding.adView.visibility = android.view.View.GONE
                        CrashLogger.log("MainActivity", "Ads hidden (premium)")
                    } else {
                        adManager.loadBannerAd(binding.adView)
                        CrashLogger.log("MainActivity", "Banner ad loading")
                    }
                }
            }
        } catch (e: Exception) {
            CrashLogger.logError("MainActivity", "ViewModel observation failed", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            adManager.destroy()
            CrashLogger.log("MainActivity", "onDestroy() - AdManager destroyed")
        } catch (e: Exception) {
            CrashLogger.logError("MainActivity", "onDestroy() failed", e)
        }
    }
}
