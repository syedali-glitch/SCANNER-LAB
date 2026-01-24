package com.plainlabs.qrpdftools.domain.model

import android.net.Uri

data class PDFDocument(
    val id: String,
    val name: String,
    val uri: Uri,
    val pageCount: Int,
    val sizeBytes: Long,
    val createdTimestamp: Long
) {
    val formattedSize: String
        get() {
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                else -> "${sizeBytes / (1024 * 1024)} MB"
            }
        }
    
    val pagesDisplay: String
        get() = if (pageCount == 1) "1 page" else "$pageCount pages"
}
