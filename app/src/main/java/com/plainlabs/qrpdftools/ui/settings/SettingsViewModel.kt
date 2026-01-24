package com.plainlabs.qrpdftools.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plainlabs.qrpdftools.data.local.AppDatabase
import com.plainlabs.qrpdftools.data.local.PreferencesManager
import com.plainlabs.qrpdftools.data.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : ViewModel() {
    
    private val scanRepository: ScanRepository
    val preferencesManager: PreferencesManager
    
    val adsRemoved: Flow<Boolean>
    
    init {
        val scanDao = AppDatabase.getDatabase(application).scanDao()
        scanRepository = ScanRepository(scanDao)
        preferencesManager = PreferencesManager(application)
        adsRemoved = preferencesManager.adsRemovedFlow
    }
    
    fun clearAllHistory() {
        viewModelScope.launch {
            scanRepository.deleteAllScans()
        }
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
