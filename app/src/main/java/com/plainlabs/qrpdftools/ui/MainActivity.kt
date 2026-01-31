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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Enable high refresh rate for smooth animations
        ErrorHandler.safe("MainActivity", Unit, "Refresh rate optimization failed") {
            ScreenUtil.enableHighRefreshRate(this)
        }
        
        logScreenInfo()
        
        adManager = AdManager(this)
        
        setupNavigation()
        observeViewModel()
        
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
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation (if exists)
        binding.bottomNavigation?.setupWithNavController(navController)
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
