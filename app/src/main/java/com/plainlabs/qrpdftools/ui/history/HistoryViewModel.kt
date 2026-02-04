package com.plainlabs.qrpdftools.ui.history

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plainlabs.qrpdftools.data.local.AppDatabase
import com.plainlabs.qrpdftools.data.repository.ScanRepository
import com.plainlabs.qrpdftools.domain.model.ScanResult
import com.plainlabs.qrpdftools.util.ShareUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : ViewModel() {
    
    private val scanRepository: ScanRepository
    private val searchQuery = MutableStateFlow("")
    
    val scans: StateFlow<List<ScanResult>>
    
    init {
        val scanDao = AppDatabase.getDatabase(application).scanDao()
        scanRepository = ScanRepository(scanDao)
        
        scans = searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    scanRepository.allScans
                } else {
                    scanRepository.searchScans(query)
                }
            }
            .map { entities ->
                entities.map { ScanResult.fromEntity(it) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun searchScans(query: String) {
        searchQuery.value = query
    }
    
    fun deleteScan(scan: ScanResult) {
        viewModelScope.launch {
            scanRepository.deleteScan(scan.toEntity())
        }
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
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
