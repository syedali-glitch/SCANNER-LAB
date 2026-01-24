package com.plainlabs.qrpdftools.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PdfProcessor(private val context: Context) {
    
    fun createPdfFromImages(images: List<Bitmap>, outputFile: File): Boolean {
        return try {
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            images.forEach { bitmap ->
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val imageData = ImageDataFactory.create(stream.toByteArray())
                val image = Image(imageData)
                
                // Fit to page
                val pageSize = pdfDocument.defaultPageSize
                image.scaleToFit(pageSize.width - 40, pageSize.height - 40)
                image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                
                document.add(image)
                
                // Add new page if not the last image
                if (bitmap != images.last()) {
                    document.add(com.itextpdf.layout.element.AreaBreak())
                }
            }
            
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun mergePdfs(pdfFiles: List<File>, outputFile: File): Boolean {
        return try {
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val mergedPdf = PdfDocument(pdfWriter)
            
            pdfFiles.forEach { file ->
                val pdfReader = PdfReader(file)
                val sourcePdf = PdfDocument(pdfReader)
                
                // Copy all pages
                sourcePdf.copyPagesTo(1, sourcePdf.numberOfPages, mergedPdf)
                
                sourcePdf.close()
            }
            
            mergedPdf.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun splitPdf(inputFile: File, startPage: Int, endPage: Int, outputFile: File): Boolean {
        return try {
            val pdfReader = PdfReader(inputFile)
            val sourcePdf = PdfDocument(pdfReader)
            
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val destinationPdf = PdfDocument(pdfWriter)
            
            // Copy specified page range
            sourcePdf.copyPagesTo(startPage, endPage, destinationPdf)
            
            destinationPdf.close()
            sourcePdf.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getPageCount(file: File): Int {
        return try {
            val pdfReader = PdfReader(file)
            val pdfDocument = PdfDocument(pdfReader)
            val pageCount = pdfDocument.numberOfPages
            pdfDocument.close()
            pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
