package com.plainlabs.qrpdftools.data.repository

import com.plainlabs.qrpdftools.data.local.dao.ScanDao
import com.plainlabs.qrpdftools.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    
    val allScans: Flow<List<ScanEntity>> = scanDao.getAllScans()
    
    val favorites: Flow<List<ScanEntity>> = scanDao.getFavorites()
    
    fun searchScans(query: String): Flow<List<ScanEntity>> {
        return scanDao.searchScans(query)
    }
    
    suspend fun getScanById(id: Long): ScanEntity? {
        return scanDao.getScanById(id)
    }
    
    suspend fun insertScan(scan: ScanEntity): Long {
        return scanDao.insertScan(scan)
    }
    
    suspend fun updateScan(scan: ScanEntity) {
        scanDao.updateScan(scan)
    }
    
    suspend fun deleteScan(scan: ScanEntity) {
        scanDao.deleteScan(scan)
    }
    
    suspend fun deleteAllScans() {
        scanDao.deleteAllScans()
    }
    
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        scanDao.toggleFavorite(id, isFavorite)
    }
}
