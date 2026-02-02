package com.plainlabs.qrpdftools.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.ads.AdManager
import com.plainlabs.qrpdftools.databinding.ActivityMainBinding
import com.plainlabs.qrpdftools.ui.main.MainViewModel
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
        
        Log.d("MainActivity", "==================== APP STARTING ====================")
        Log.d("MainActivity", "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d("MainActivity", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        try {
            Log.d("MainActivity", "Step 1: Inflating layout...")
            binding = ActivityMainBinding.inflate(layoutInflater)
            
            Log.d("MainActivity", "Step 2: Setting content view...")
            setContentView(binding.root)
            
            Log.d("MainActivity", "Step 3: Initializing AdManager...")
            adManager = AdManager(this)
            
            Log.d("MainActivity", "Step 4: Setting up navigation...")
            setupNavigation()
            
            Log.d("MainActivity", "Step 5: Observing ViewModel...")
            observeViewModel()
            
            Log.d("MainActivity", "Step 6: Preloading ads...")
            adManager.preloadInterstitialAd()
            adManager.preloadRewardedAd()
            
            Log.d("MainActivity", "==================== APP STARTED SUCCESSFULLY ====================")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "==================== CRITICAL ERROR ====================", e)
            e.printStackTrace()
            
            // Show error to user
            try {
                val errorText = "App failed to start: ${e.message}\n\n${e.stackTraceToString()}"
                Log.e("MainActivity", errorText)
                
                android.widget.Toast.makeText(
                    this,
                    "Initialization failed. Check logs.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (e2: Exception) {
                Log.e("MainActivity", "Toast failed", e2)
            }
        }
    }
    
    private fun setupNavigation() {
        try {
            Log.d("MainActivity", "Finding NavHostFragment...")
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            if (navHostFragment == null) {
                Log.e("MainActivity", "ERROR: NavHostFragment not found in layout!")
                return
            }
            
            Log.d("MainActivity", "Getting NavController...")
            navController = navHostFragment.navController
            
            Log.d("MainActivity", "Setting up bottom navigation...")
            binding.bottomNavigation.setupWithNavController(navController)
            
            Log.d("MainActivity", "Navigation setup complete ✓")
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation setup FAILED", e)
            e.printStackTrace()
        }
    }
    
    private fun observeViewModel() {
        try {
            lifecycleScope.launch {
                viewModel.adsRemovedFlow.collect { adsRemoved ->
                    if (adsRemoved) {
                        binding.adView.visibility = android.view.View.GONE
                        Log.d("MainActivity", "Ads hidden (premium user)")
                    } else {
                        adManager.loadBannerAd(binding.adView)
                        Log.d("MainActivity", "Banner ad loading...")
                    }
                }
            }
            Log.d("MainActivity", "ViewModel observer setup complete ✓")
        } catch (e: Exception) {
            Log.e("MainActivity", "ViewModel observation FAILED", e)
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            adManager.destroy()
            Log.d("MainActivity", "AdManager destroyed")
        } catch (e: Exception) {
            Log.e("MainActivity", "AdManager destroy failed", e)
        }
    }
}
