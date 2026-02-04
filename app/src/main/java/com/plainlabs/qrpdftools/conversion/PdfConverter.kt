package com.plainlabs.qrpdftools.conversion

import android.content.Context
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * PdfConverter - Refactored to Proprietary PlainLabs engine.
 * 
 * UX Pivot: This is now an EXTRACTION engine, not a conversion engine.
 * Mandatory Architect Comment: OpenPDF is a stream reader, not a layout engine.
 * We extract raw text paragraphs only. No complex layout reconstruction attempted.
 */
object PdfConverter {

    /**
     * Extracts raw text from PDF and saves it as a Word document.
     */
    fun extractPdfTextToWord(inputPath: String, outputPath: String, context: Context): Boolean {
        return try {
            val pdfFile = File(inputPath)
            val docxFile = File(outputPath)
            
            val sb = StringBuilder()
            val reader = PdfReader(pdfFile.absolutePath)
            val n = reader.numberOfPages
            
            val extractor = PdfTextExtractor(reader)
            for (i in 1..n) {
                val text = extractor.getTextFromPage(i)
                sb.append(text).append("\n\n")
            }
            reader.close()
            
            val document = XWPFDocument()
            sb.toString().split("\n").forEach { line ->
                val paragraph = document.createParagraph()
                val run = paragraph.createRun()
                run.setText(line)
            }
            
            FileOutputStream(docxFile).use { out ->
                document.write(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Converts images to PDF using the memory-safe NativePdfGenerator.
     */
    suspend fun imageToPdf(imagePaths: List<String>, outputPath: String): Boolean {
         return try {
            val imageFiles = imagePaths.map { File(it) }
            NativePdfGenerator.generatePdfFromImages(imageFiles, File(outputPath)).isSuccess
         } catch (e: Exception) {
             e.printStackTrace()
             false
         }
    }
}