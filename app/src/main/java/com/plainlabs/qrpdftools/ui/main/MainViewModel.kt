package com.plainlabs.qrpdftools.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plainlabs.qrpdftools.data.local.AppDatabase
import com.plainlabs.qrpdftools.data.local.PreferencesManager
import com.plainlabs.qrpdftools.data.local.entity.ScanEntity
import com.plainlabs.qrpdftools.data.local.entity.ScanType
import com.plainlabs.qrpdftools.data.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : ViewModel() {
    
    private val scanRepository: ScanRepository
    private val preferencesManager: PreferencesManager
    
    val adsRemovedFlow: Flow<Boolean>
    
    init {
        val scanDao = AppDatabase.getDatabase(application).scanDao()
        scanRepository = ScanRepository(scanDao)
        preferencesManager = PreferencesManager(application)
        adsRemovedFlow = preferencesManager.adsRemovedFlow
    }
    
    fun saveScan(content: String, type: ScanType, format: String) {
        viewModelScope.launch {
            val scan = ScanEntity(
                content = content,
                type = type,
                format = format,
                rawValue = content
            )
            scanRepository.insertScan(scan)
        }
    }
    
    suspend fun isFirstScan(): Boolean {
        val scans = scanRepository.allScans.first()
        return scans.size == 1
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
