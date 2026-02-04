package com.plainlabs.qrpdftools.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val content: String,
    val type: ScanType,
    val format: String, // QR_CODE, CODE_128, EAN_13, etc.
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val rawValue: String = content,
    
    // Phase 2: High-End Architecture Fields
    val fileName: String? = null,
    val creationDate: Long = timestamp,
    val tag: String? = "Personal" // Default tag
)

enum class ScanType {
    QR_CODE,
    BARCODE,
    PDF
}
