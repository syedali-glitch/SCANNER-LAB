package com.scanner.lab.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton to manage User Premium/Pro Status
 * "Monetization Stub" - Currently uses local preferences but designed for Billing Client
 */
object UserPremiums {

    private const val PREFS_NAME = "plainlabs_premiums"
    private const val KEY_IS_PRO = "is_pro_user"

    var isPro: Boolean = false
        private set

    fun init(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isPro = prefs.getBoolean(KEY_IS_PRO, false) // Default to free
    }

    fun setProStatus(context: Context, status: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PRO, status).apply()
        isPro = status
    }
    
    /**
     * Shows Upsell Dialog (stub) or returns false if blocked
     */
    fun checkOrShowUpsell(context: Context): Boolean {
        if (isPro) return true
        
        // TODO: Show comprehensive Upsell Dialog (or start Activity)
        // For now, just a Toast or simple false
        android.widget.Toast.makeText(context, "Upgrade to Pro for this feature!", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
}
