package com.plainlabs.qrpdftools.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.plainlabs.qrpdftools.R
import com.plainlabs.qrpdftools.billing.BillingManager
import com.plainlabs.qrpdftools.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(requireActivity().application)
    }

    private lateinit var billingManager: BillingManager
    private var removeAdsProduct: ProductDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        billingManager = BillingManager(
            requireContext(),
            viewModel.preferencesManager
        )

        setupListeners()
        observeViewModel()
        loadProductDetails()
    }

    private fun setupListeners() {
        binding.btnRemoveAds.setOnClickListener {
            removeAdsProduct?.let { product ->
                billingManager.launchPurchaseFlow(
                    requireActivity(),
                    product,
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            R.string.ads_removed,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            "${getString(R.string.purchase_failed)}: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }

        binding.btnRestorePurchase.setOnClickListener {
            billingManager.queryPurchases()
            Toast.makeText(
                requireContext(),
                "Checking purchases...",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearAllHistory()
            Toast.makeText(
                requireContext(),
                "History cleared",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.adsRemoved.collect { removed ->
                if (removed) {
                    binding.btnRemoveAds.isEnabled = false
                    binding.btnRemoveAds.text = "Ads Removed ✓"
                }
            }
        }
    }

    private fun loadProductDetails() {
        billingManager.queryProductDetails { products ->
            removeAdsProduct = products.find {
                it.productId == BillingManager.REMOVE_ADS_PRODUCT_ID
            }

            removeAdsProduct?.let { product ->
                val price = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "$2.99"
                binding.btnRemoveAds.text = "Remove Ads - $price"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingManager.destroy()
        _binding = null
    }
}