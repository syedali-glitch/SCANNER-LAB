package com.plainlabs.qrpdftools.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.plainlabs.qrpdftools.ads.AdManager
import com.plainlabs.qrpdftools.databinding.DialogRewardedFeatureBinding

class RewardedFeatureDialog(
    context: Context,
    private val featureName: String,
    private val featureDescription: String,
    private val adManager: AdManager,
    private val onRewardEarned: () -> Unit,
    private val onRemoveAdsClick: () -> Unit
) : Dialog(context) {
    
    private lateinit var binding: DialogRewardedFeatureBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogRewardedFeatureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        binding.tvTitle.text = "Unlock $featureName"
        binding.tvDescription.text = featureDescription
        
        // Check if ad is ready
        if (!adManager.isRewardedAdReady()) {
            binding.btnWatchAd.isEnabled = false
            binding.btnWatchAd.text = "Loading ad..."
        }
    }
    
    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnWatchAd.setOnClickListener {
            adManager.showRewardedAd(
                activity = context as Activity,
                onRewardEarned = {
                    onRewardEarned()
                    dismiss()
                },
                onAdDismissed = {
                    // Ad was dismissed without reward
                    dismiss()
                }
            )
        }
        
        binding.tvRemoveAds.setOnClickListener {
            onRemoveAdsClick()
            dismiss()
        }
    }
}
