package com.scanner.lab.converters

import com.scanner.lab.utils.ErrorHandler
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Bidirectional DOCX converter
 */
object DocxConverter {
    
    /**
     * DOCX to PDF
     */
    fun docxToPdf(
        docxPath: String,
        outputPdfPath: String
    ): Result<File> = ErrorHandler.safe("DocxToPdf") {
        val outputFile = File(outputPdfPath)
        val docxFile = File(docxPath)
        
        FileInputStream(docxFile).use { fis ->
            val document = XWPFDocument(fis)
            
            PDDocument().use { pdfDoc ->
                val page = PDPage(PDRectangle.A4)
                pdfDoc.addPage(page)
                
                PDPageContentStream(pdfDoc, page).use { contentStream ->
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)
                    contentStream.setLeading(14.5f)
                    contentStream.newLineAtOffset(50f, page.mediaBox.height - 50)
                    
                    // Extract paragraphs
                    document.paragraphs.forEach { paragraph ->
                        val text = paragraph.text
                        if (text.isNotBlank()) {
                            contentStream.showText(text)
                            contentStream.newLine()
                        }
                    }
                    
                    contentStream.endText()
                }
                
                pdfDoc.save(outputFile)
            }
            
            document.close()
        }
        
        outputFile
    }
    
    /**
     * PDF to DOCX (OCR-based)
     */
    suspend fun pdfToDocx(
        pdfPath: String,
        outputDocxPath: String
    ): Result<File> = ErrorHandler.safe("PdfToDocx") {
        val outputFile = File(outputDocxPath)
        
        // Extract text from PDF
        val text = TextConverter.pdfToText(pdfPath).getOrThrow()
        
        // Create DOCX
        textToDocx(text, outputDocxPath).getOrThrow()
        
        outputFile
    }
    
    /**
     * Text to DOCX
     */
    fun textToDocx(
        text: String,
        outputDocxPath: String
    ): Result<File> = ErrorHandler.safe("TextToDocx") {
        val outputFile = File(outputDocxPath)
        
        XWPFDocument().use { document ->
            // Split text into paragraphs
            text.split("\n").forEach { line ->
                val paragraph = document.createParagraph()
                val run = paragraph.createRun()
                run.setText(line)
                run.fontSize = 12
                run.fontFamily = "Arial"
            }
            
            FileOutputStream(outputFile).use { fos ->
                document.write(fos)
            }
        }
        
        outputFile
    }
    
    /**
     * DOCX to Text
     */
    fun docxToText(docxPath: String): Result<String> = ErrorHandler.safe("DocxToText") {
        val docxFile = File(docxPath)
        val textBuilder = StringBuilder()
        
        FileInputStream(docxFile).use { fis ->
            val document = XWPFDocument(fis)
            
            document.paragraphs.forEach { paragraph ->
                textBuilder.append(paragraph.text)
                textBuilder.append("\n")
            }
            
            document.close()
        }
        
        textBuilder.toString()
    }
}
