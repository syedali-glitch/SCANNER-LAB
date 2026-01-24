package com.plainlabs.qrpdftools.data.local.dao

import androidx.room.*
import com.plainlabs.qrpdftools.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>
    
    @Query("SELECT * FROM scans WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<ScanEntity>>
    
    @Query("SELECT * FROM scans WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScans(query: String): Flow<List<ScanEntity>>
    
    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getScanById(id: Long): ScanEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long
    
    @Update
    suspend fun updateScan(scan: ScanEntity)
    
    @Delete
    suspend fun deleteScan(scan: ScanEntity)
    
    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()
    
    @Query("UPDATE scans SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}
