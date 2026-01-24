package com.plainlabs.qrpdftools.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.plainlabs.qrpdftools.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AdManager(private val context: Context) {
    
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private val preferencesManager = PreferencesManager(context)
    
    companion object {
        private const val TAG = "AdManager"
        // Test Ad Unit IDs - Replace with your own for production
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
    
    private var rewardedAd: com.google.android.gms.ads.rewarded.RewardedAd? = null
    private var isLoadingRewarded = false
    
    init {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
    }
    
    fun shouldShowAds(): Boolean {
        return runBlocking {
            !preferencesManager.adsRemovedFlow.first()
        }
    }
    
    fun loadBannerAd(adView: AdView) {
        if (!shouldShowAds()) {
            adView.visibility = android.view.View.GONE
            return
        }
        
        adView.visibility = android.view.View.VISIBLE
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded")
            }
            
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Banner ad failed to load: ${adError.message}")
                adView.visibility = android.view.View.GONE
            }
        }
    }
    
    fun preloadInterstitialAd() {
        if (!shouldShowAds() || isLoadingInterstitial || interstitialAd != null) {
            return
        }
        
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Interstitial ad dismissed")
                            interstitialAd = null
                            // Preload next ad
                            preloadInterstitialAd()
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                            interstitialAd = null
                        }
                    }
                }
                
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }
    
    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        if (!shouldShowAds()) {
            onAdClosed()
            return
        }
        
        interstitialAd?.let { ad ->
            ad.show(activity)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdClosed()
                    interstitialAd = null
                    preloadInterstitialAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdClosed()
                    interstitialAd = null
                }
            }
        } ?: run {
            // Ad not ready, continue without showing
            onAdClosed()
            preloadInterstitialAd()
        }
    }
    
    fun preloadRewardedAd() {
        if (!shouldShowAds() || isLoadingRewarded || rewardedAd != null) {
            return
        }
        
        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        
        com.google.android.gms.ads.rewarded.RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: com.google.android.gms.ads.rewarded.RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    isLoadingRewarded = false
                }
                
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${adError.message}")
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }
    
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        if (!shouldShowAds()) {
            // User has removed ads, grant reward immediately
            onRewardEarned()
            return
        }
        
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    onAdDismissed()
                    rewardedAd = null
                    preloadRewardedAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                    rewardedAd = null
                    onAdDismissed()
                }
            }
            
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            }
        } ?: run {
            // Ad not ready, don't grant reward
            Log.w(TAG, "Rewarded ad not ready")
            onAdDismissed()
            preloadRewardedAd()
        }
    }
    
    fun isRewardedAdReady(): Boolean {
        return rewardedAd != null || !shouldShowAds()
    }
    
    fun destroy() {
        interstitialAd = null
        rewardedAd = null
    }
}
