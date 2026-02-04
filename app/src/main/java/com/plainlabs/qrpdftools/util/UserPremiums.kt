package com.plainlabs.qrpdftools.util

import android.content.Context
import android.content.SharedPreferences

/**
 * UserPremiums - PlainLabs Monetization Engine.
 * 
 * Manages Pro/Premium status.
 */
object UserPremiums {

    private const val PREFS_NAME = "plainlabs_premiums"
    private const val KEY_IS_PRO = "is_pro_user"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the user has Pro status.
     * Pivot: All advanced conversions (OCR, High-End Filters) are gated by this check.
     */
    fun isPro(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PRO, false)
    }

    /**
     * Sets the Pro status for the user.
     */
    fun setPro(context: Context, isPro: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }
}
