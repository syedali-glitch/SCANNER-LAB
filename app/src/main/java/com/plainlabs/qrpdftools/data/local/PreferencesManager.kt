package com.plainlabs.qrpdftools.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val ADS_REMOVED_KEY = booleanPreferencesKey("ads_removed")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
        private val PREMIUM_UNLOCKED_KEY = booleanPreferencesKey("premium_unlocked")
    }
    
    val adsRemovedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ADS_REMOVED_KEY] ?: false
        }
    
    suspend fun setAdsRemoved(removed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ADS_REMOVED_KEY] = removed
        }
    }
    
    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FIRST_LAUNCH_KEY] ?: true
        }
    
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = false
        }
    }
    
    val premiumUnlockedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PREMIUM_UNLOCKED_KEY] ?: false
        }
    
    suspend fun setPremiumUnlocked(unlocked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREMIUM_UNLOCKED_KEY] = unlocked
        }
    }
}
