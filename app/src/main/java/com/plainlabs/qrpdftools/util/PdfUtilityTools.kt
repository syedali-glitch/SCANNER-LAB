package com.plainlabs.qrpdftools.util

import android.content.Context
import com.lowagie.text.Element
import com.lowagie.text.pdf.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * PdfUtilityTools - Refactored to use OpenPDF.
 * Mandate: Proprietary-friendly licensing and IO-safe threading.
 */
object PdfUtilityTools {

    /**
     * Attempts to compress PDF by enabling full compression in OpenPDF.
     * Mandate: Offloaded to Dispatchers.IO.
     */
    suspend fun compressPdf(inputPath: File, outputPath: File, quality: Float = 0.5f, removeMetadata: Boolean = false): Float = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputPath.absolutePath)
            val stamper = PdfStamper(reader, FileOutputStream(outputPath))
            
            // OpenPDF Full Compression
            stamper.setFullCompression()
            
            if (removeMetadata) {
                val info = reader.info
                info.clear()
                stamper.moreInfo = info
            }

            stamper.close()
            reader.close()
            
            val originalSize = inputPath.length()
            val newSize = outputPath.length()
            if (originalSize == 0L) 0f else 1.0f - (newSize.toFloat() / originalSize.toFloat())
        } catch (e: Exception) {
            throw IOException("Compression failed: ${e.message}")
        }
    }

    /**
     * Adds a watermark to each page of the PDF using OpenPDF.
     */
    suspend fun watermarkPdf(inputPath: File, outputPath: File, watermarkText: String, opacity: Float = 0.3f, diagonal: Boolean = true) = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputPath.absolutePath)
            val stamper = PdfStamper(reader, FileOutputStream(outputPath))
            val font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED)
            
            val n = reader.numberOfPages
            for (i in 1..n) {
                val over = stamper.getOverContent(i)
                over.saveState()
                val gState = PdfGState()
                gState.fillOpacity = opacity
                over.setGState(gState)
                
                over.beginText()
                over.setFontAndSize(font, 50f)
                over.setRGBColorFill(211, 211, 211) // Light Gray
                
                val pageSize = reader.getPageSizeWithRotation(i)
                val x = pageSize.width / 2
                val y = pageSize.height / 2
                val rotation = if (diagonal) 45f else 0f
                
                over.showTextAligned(Element.ALIGN_CENTER, watermarkText, x, y, rotation)
                over.endText()
                over.restoreState()
            }
            stamper.close()
            reader.close()
        } catch (e: Exception) {
            throw IOException("Watermarking failed: ${e.message}")
        }
    }

    /**
     * Protects a PDF with a user and owner password using OpenPDF encryption.
     */
    suspend fun protectPdf(inputPath: File, outputPath: File, userPass: String, ownerPass: String) = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(inputPath.absolutePath)
            val stamper = PdfStamper(reader, FileOutputStream(outputPath))
            
            stamper.setEncryption(
                userPass.toByteArray(),
                ownerPass.toByteArray(),
                PdfWriter.ALLOW_PRINTING,
                PdfWriter.ENCRYPTION_AES_128
            )
            
            stamper.close()
            reader.close()
        } catch (e: Exception) {
            throw IOException("Password protection failed: ${e.message}")
        }
    }
}