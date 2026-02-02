package com.plainlabs.qrpdftools.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.ads.AdManager
import com.plainlabs.qrpdftools.databinding.ActivityMainBinding
import com.plainlabs.qrpdftools.ui.main.MainViewModel
import com.plainlabs.qrpdftools.util.ErrorHandler
import com.plainlabs.qrpdftools.util.ScreenUtil
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
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Enable high refresh rate for smooth animations
            try {
                ScreenUtil.enableHighRefreshRate(this)
            } catch (e: Exception) {
                Log.w("MainActivity", "High refresh rate not supported: ${e.message}")
            }
            
            try {
                logScreenInfo()
            } catch (e: Exception) {
                Log.w("MainActivity", "Screen info logging failed: ${e.message}")
            }
            
            adManager = AdManager(this)
            
            setupNavigation()
            observeViewModel()
            
            // Preload ads
            try {
                adManager.preloadInterstitialAd()
                adManager.preloadRewardedAd()
            } catch (e: Exception) {
                Log.w("MainActivity", "Ad preloading failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error in onCreate", e)
            e.printStackTrace()
            // Don't crash, try to show error to user
            try {
                android.widget.Toast.makeText(
                    this,
                    "App initialization failed: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (toastError: Exception) {
                // Even toast failed, just log
                Log.e("MainActivity", "Toast also failed", toastError)
            }
        }
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
    
    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            if (navHostFragment == null) {
                Log.e("MainActivity", "NavHostFragment not found!")
                return
            }
            
            navController = navHostFragment.navController
            
            // Setup bottom navigation
            binding.bottomNavigation.setupWithNavController(navController)
            
            Log.d("MainActivity", "Navigation setup complete")
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation setup failed", e)
            e.printStackTrace()
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
    
    override fun onDestroy() {
        super.onDestroy()
        adManager.destroy()
    }
}
