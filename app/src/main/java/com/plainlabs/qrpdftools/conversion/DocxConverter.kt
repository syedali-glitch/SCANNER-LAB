package com.plainlabs.qrpdftools.conversion

import org.apache.poi.xwpf.usermodel.XWPFDocument
import com.lowagie.text.Document
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * DocxConverter - Refactored to use OpenPDF.
 */
object DocxConverter {

    /**
     * Converts a Word (.docx) file to PDF using Apache POI and OpenPDF.
     */
    fun docxToPdf(inputPath: String, outputPath: String): Boolean {
        return try {
            FileInputStream(inputPath).use { fis ->
                val document = XWPFDocument(fis)
                val outDocument = Document()
                PdfWriter.getInstance(outDocument, FileOutputStream(outputPath))
                outDocument.open()
                
                // Iterate paragraphs
                for (para in document.paragraphs) {
                    if (para.text.isNotEmpty()) {
                        outDocument.add(Paragraph(para.text))
                    }
                }
                
                outDocument.close()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}