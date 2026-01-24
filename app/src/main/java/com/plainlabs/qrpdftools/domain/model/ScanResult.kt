package com.plainlabs.qrpdftools.domain.model

import com.plainlabs.qrpdftools.data.local.entity.ScanEntity
import com.plainlabs.qrpdftools.data.local.entity.ScanType
import java.text.SimpleDateFormat
import java.util.*

data class ScanResult(
    val id: Long,
    val content: String,
    val type: ScanType,
    val format: String,
    val timestamp: Long,
    val isFavorite: Boolean,
    val rawValue: String
) {
    val formattedTimestamp: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    
    val shortContent: String
        get() = if (content.length > 50) {
            content.substring(0, 50) + "..."
        } else {
            content
        }
    
    val typeDisplay: String
        get() = when (type) {
            ScanType.QR_CODE -> "QR Code"
            ScanType.BARCODE -> "Barcode"
            ScanType.PDF -> "PDF"
        }
    
    companion object {
        fun fromEntity(entity: ScanEntity): ScanResult {
            return ScanResult(
                id = entity.id,
                content = entity.content,
                type = entity.type,
                format = entity.format,
                timestamp = entity.timestamp,
                isFavorite = entity.isFavorite,
                rawValue = entity.rawValue
            )
        }
    }
    
    fun toEntity(): ScanEntity {
        return ScanEntity(
            id = id,
            content = content,
            type = type,
            format = format,
            timestamp = timestamp,
            isFavorite = isFavorite,
            rawValue = rawValue
        )
    }
}
