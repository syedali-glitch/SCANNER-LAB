package com.plainlabs.qrpdftools.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.plainlabs.qrpdftools.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : PurchasesUpdatedListener {
    
    private var billingClient: BillingClient? = null
    private var onPurchaseSuccess: (() -> Unit)? = null
    private var onPurchaseFailed: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "BillingManager"
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
        const val PREMIUM_PRODUCT_ID = "premium_features"
    }
    
    init {
        setupBillingClient()
    }
    
    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing client disconnected")
                // Retry connection
                setupBillingClient()
            }
        })
    }
    
    fun queryProductDetails(onDetailsLoaded: (List<ProductDetails>) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Product details loaded: ${productDetailsList.size}")
                onDetailsLoaded(productDetailsList)
            } else {
                Log.e(TAG, "Failed to load product details: ${billingResult.debugMessage}")
                onDetailsLoaded(emptyList())
            }
        }
    }
    
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        this.onPurchaseSuccess = onSuccess
        this.onPurchaseFailed = onFailure
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult?.debugMessage}")
            onFailure(billingResult?.debugMessage ?: "Unknown error")
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                onPurchaseFailed?.invoke("Purchase canceled")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                onPurchaseFailed?.invoke(billingResult.debugMessage)
            }
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
            
            // Update local state
            CoroutineScope(Dispatchers.IO).launch {
                when {
                    purchase.products.contains(REMOVE_ADS_PRODUCT_ID) -> {
                        preferencesManager.setAdsRemoved(true)
                    }
                    purchase.products.contains(PREMIUM_PRODUCT_ID) -> {
                        preferencesManager.setPremiumUnlocked(true)
                    }
                }
            }
            
            onPurchaseSuccess?.invoke()
            Log.d(TAG, "Purchase completed: ${purchase.products}")
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            }
        }
    }
    
    fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                CoroutineScope(Dispatchers.IO).launch {
                    var adsRemoved = false
                    var premiumUnlocked = false
                    
                    purchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            if (purchase.products.contains(REMOVE_ADS_PRODUCT_ID)) {
                                adsRemoved = true
                            }
                            if (purchase.products.contains(PREMIUM_PRODUCT_ID)) {
                                premiumUnlocked = true
                            }
                        }
                    }
                    
                    preferencesManager.setAdsRemoved(adsRemoved)
                    preferencesManager.setPremiumUnlocked(premiumUnlocked)
                }
                Log.d(TAG, "Purchases restored")
            }
        }
    }
    
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
