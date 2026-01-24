package com.plainlabs.qrpdftools.ui.favorites

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plainlabs.qrpdftools.data.local.AppDatabase
import com.plainlabs.qrpdftools.data.repository.ScanRepository
import com.plainlabs.qrpdftools.domain.model.ScanResult
import com.plainlabs.qrpdftools.util.ShareUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : ViewModel() {
    
    private val scanRepository: ScanRepository
    
    val favorites: StateFlow<List<ScanResult>>
    
    init {
        val scanDao = AppDatabase.getDatabase(application).scanDao()
        scanRepository = ScanRepository(scanDao)
        
        favorites = scanRepository.favorites
            .map { entities ->
                entities.map { ScanResult.fromEntity(it) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun toggleFavorite(scan: ScanResult) {
        viewModelScope.launch {
            scanRepository.toggleFavorite(scan.id, !scan.isFavorite)
        }
    }
    
    fun copyScanToClipboard(context: Context, scan: ScanResult) {
        ShareUtil.copyToClipboard(context, scan.content)
    }
    
    fun shareScan(context: Context, scan: ScanResult) {
        ShareUtil.shareText(context, scan.content)
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
                return FavoritesViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
