package com.plainlabs.qrpdftools.util

import android.content.Context
import android.graphics.Bitmap
import com.lowagie.text.Document
import com.lowagie.text.pdf.PdfCopy
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * PdfProcessor - Refactored to use OpenPDF and Native Android PDF APIs.
 * Eliminates AGPL iText dependency.
 */
class PdfProcessor(private val context: Context) {
    
    /**
     * Creates PDF from images using the proprietary NativePdfGenerator.
     * Uses Architect-mandated memory safeguards.
     */
    suspend fun createPdfFromImages(imageFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        NativePdfGenerator.generatePdfFromImages(imageFiles, outputFile).isSuccess
    }
    
    /**
     * Merges multiple PDF files using OpenPDF PdfCopy.
     * Mandate: Offloaded to Dispatchers.IO.
     */
    suspend fun mergePdfs(pdfFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var document: Document? = null
        try {
            document = Document()
            val copy = PdfCopy(document, FileOutputStream(outputFile))
            document.open()
            
            pdfFiles.forEach { file ->
                val reader = PdfReader(file.absolutePath)
                val n = reader.numberOfPages
                for (i in 1..n) {
                    copy.addPage(copy.getImportedPage(reader, i))
                }
                reader.close()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document?.close()
        }
    }
    
    /**
     * Splits a PDF file into a new file containing a specific page range.
     */
    suspend fun splitPdf(inputFile: File, startPage: Int, endPage: Int, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var document: Document? = null
        try {
            val reader = PdfReader(inputFile.absolutePath)
            document = Document()
            val copy = PdfCopy(document, FileOutputStream(outputFile))
            document.open()
            
            for (i in startPage..endPage) {
                if (i in 1..reader.numberOfPages) {
                    copy.addPage(copy.getImportedPage(reader, i))
                }
            }
            
            reader.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document?.close()
        }
    }
    
    /**
     * Rotates all pages in a PDF by the specified degrees (e.g. 90, 180, 270).
     */
    suspend fun rotatePdf(inputFile: File, outputFile: File, rotationDegrees: Int): Boolean = withContext(Dispatchers.IO) {
        var reader: PdfReader? = null
        var stamper: PdfStamper? = null
        try {
            reader = PdfReader(inputFile.absolutePath)
            stamper = PdfStamper(reader, FileOutputStream(outputFile))
            val n = reader.numberOfPages
            
            for (i in 1..n) {
                val pageDict = reader.getPageN(i)
                val rotation = pageDict.getAsNumber(PdfName.ROTATE)
                val oldRotation = rotation?.intValue() ?: 0
                pageDict.put(PdfName.ROTATE, PdfNumber((oldRotation + rotationDegrees) % 360))
            }
            
            stamper.close()
            reader.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { stamper?.close() } catch (e: Exception) {}
            try { reader?.close() } catch (e: Exception) {}
        }
    }

    /**
     * Utility to get total page count of a PDF using OpenPDF.
     */
    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(file.absolutePath)
            val count = reader.numberOfPages
            reader.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
