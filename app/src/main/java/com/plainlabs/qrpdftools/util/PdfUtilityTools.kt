package com.plainlabs.qrpdftools.util

import android.content.Context
import android.graphics.Bitmap
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfUtilityTools {

    fun compressPdf(inputPath: File, outputPath: File, quality: Float = 0.5f, removeMetadata: Boolean = false): Float {
         try {
            val writerProperties = WriterProperties()
            if (quality < 0.8f) {
                writerProperties.setCompressionLevel(9) // Max compression
            } else {
                 writerProperties.setCompressionLevel(5)
            }
            
            val pdfReader = PdfReader(inputPath)
            val pdfWriter = PdfWriter(FileOutputStream(outputPath), writerProperties)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)
            
            if (removeMetadata) {
                pdfDoc.documentInfo.title = ""
                pdfDoc.documentInfo.author = ""
                pdfDoc.documentInfo.subject = ""
                pdfDoc.documentInfo.creator = ""
            }

            // We could iterate objects and downsample images here for true "quality" control
            // For now, stream compression is enabled by default in iText7.

            pdfDoc.close()
            
            val originalSize = inputPath.length()
            val newSize = outputPath.length()
            return 1.0f - (newSize.toFloat() / originalSize.toFloat())
         } catch (e: Exception) {
             throw IOException("Compression failed: ${e.message}")
         }
    }

    fun watermarkPdf(inputPath: File, outputPath: File, watermarkText: String, opacity: Float = 0.3f, diagonal: Boolean = true) {
        try {
            val pdfDoc = PdfDocument(PdfReader(inputPath), PdfWriter(FileOutputStream(outputPath)))
            val n = pdfDoc.numberOfPages
            val font = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)
            val gs1 = PdfExtGState().setFillOpacity(opacity)

            for (i in 1..n) {
                val page = pdfDoc.getPage(i)
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page)
                
                canvas.saveState()
                canvas.setExtGState(gs1)
                
                val doc = Document(pdfDoc)
                
                val x = pageSize.width / 2
                val y = pageSize.height / 2
                
                val paragraph = Paragraph(watermarkText)
                    .setFont(font)
                    .setFontSize(50f)
                    
                // This uses layout, but drawing on canvas directly is often easier for rotation
                // canvas.beginText() ...
                
                // Let's use simple canvas drawing for rotation
                canvas.beginText()
                canvas.setFontAndSize(font, 50f)
                canvas.setColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY, true)
                
                // Calculate matrix for rotation
                // For simplicity, just show text at center. 
                // iText7 ShowTextAligned handles rotation.
                
                com.itextpdf.layout.Canvas(canvas, pageSize)
                    .showTextAligned(paragraph, x, y, i, TextAlignment.CENTER, VerticalAlignment.MIDDLE, if (diagonal) 45f else 0f)

                canvas.restoreState()
            }
            pdfDoc.close()
        } catch (e: Exception) {
             throw IOException("Watermarking failed: ${e.message}")
        }
    }

    fun protectPdf(inputPath: File, outputPath: File, userPass: String, ownerPass: String) {
        try {
            val props = WriterProperties()
            props.setStandardEncryption(
                userPass.toByteArray(),
                ownerPass.toByteArray(),
                com.itextpdf.kernel.pdf.EncryptionConstants.ALLOW_PRINTING,
                com.itextpdf.kernel.pdf.EncryptionConstants.ENCRYPTION_AES_128
            )
            
            val pdfDoc = PdfDocument(PdfReader(inputPath), PdfWriter(FileOutputStream(outputPath), props))
            pdfDoc.close()
        } catch (e: Exception) {
             throw IOException("Password protection failed: ${e.message}")
        }
    }
}