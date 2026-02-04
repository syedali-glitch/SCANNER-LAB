package com.plainlabs.qrpdftools.conversion

import android.content.Context
import com.lowagie.text.Document
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.pdf.parser.PdfTextExtractor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * TextConverter - Refactored to use OpenPDF.
 */
class TextConverter(private val context: Context) {

    private val ocrEngine = OcrEngine(context)

    /**
     * Extracts text from PDF using OpenPDF.
     */
    fun convertPdfToText(pdfFile: File, outputText: File, useOcr: Boolean, callback: (Float) -> Unit) {
        try {
            val sb = StringBuilder()
            val reader = PdfReader(pdfFile.absolutePath)
            val pageCount = reader.numberOfPages
            
            val extractor = PdfTextExtractor(reader)
            for (i in 1..pageCount) {
                val text = extractor.getTextFromPage(i)
                if (text.isNotBlank()) {
                    sb.append(text).append("\n\n")
                }
                callback((i).toFloat() / pageCount * 0.5f)
            }
            
            reader.close()
            val extractedText = sb.toString()
            
            if ((extractedText.length < 50 && useOcr) || useOcr) {
                ocrEngine.extractTextFromPdf(pdfFile) { progress, text ->
                     callback(0.5f + (progress * 0.5f))
                     outputText.writeText(text)
                }
            } else {
                outputText.writeText(extractedText)
                callback(1.0f)
            }
            
        } catch (e: Exception) {
            throw IOException("PDF to Text failed: ${e.message}")
        }
    }

    /**
     * Converts text to PDF using OpenPDF.
     */
    fun convertTextToPdf(textFile: File, outputPdf: File, callback: (Float) -> Unit) {
        try {
            val text = textFile.readText()
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(outputPdf))
            document.open()
            
            val lines = text.split("\n")
            for ((index, line) in lines.withIndex()) {
                if (line.isNotBlank()) {
                    document.add(Paragraph(line))
                } else {
                    document.add(Paragraph("\n"))
                }
                
                if (index % 100 == 0) {
                     callback(index.toFloat() / lines.size)
                }
            }
            
            document.close()
            callback(1.0f)
        } catch (e: Exception) {
             throw IOException("Text to PDF failed: ${e.message}")
        }
    }
}