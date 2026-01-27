package com.plainlabs.qrpdftools.conversion

import android.content.Context
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfConverter {

    fun pdfToDocx(inputPath: String, outputPath: String, context: Context): Boolean {
        return try {
            val pdfFile = File(inputPath)
            val docxFile = File(outputPath)
            
            // Text extraction approach
            val textConverter = TextConverter(context)
            val tempTextFile = File(context.cacheDir, "temp_pdf_to_docx.txt")
            
            // Use existing TextConverter logic
            // We need to wait for callback or modify TextConverter to be synchronous or us runBlocking
            // For simplicity in this object, we'll reimplement simple extraction or wrapping
            
            val sb = StringBuilder()
            val pdfDoc = PdfDocument(PdfReader(pdfFile))
            val n = pdfDoc.numberOfPages
            for (i in 1..n) {
                val page = pdfDoc.getPage(i)
                val text = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page)
                sb.append(text).append("\n\n")
            }
            pdfDoc.close()
            
            val document = XWPFDocument()
            // Split by lines and add paragraphs
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

    fun imageToPdf(imagePaths: List<String>, outputPath: String): Boolean {
         return try {
            val pdfWriter = PdfWriter(FileOutputStream(outputPath))
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)
            
            for (path in imagePaths) {
                val imageData = ImageDataFactory.create(path)
                val image = Image(imageData)
                // Fit to A4 or Original?
                // Defaulting to add image
                document.add(image)
                document.add(com.itextpdf.layout.element.AreaBreak(com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE))
            }
            document.close()
            true
         } catch (e: Exception) {
             e.printStackTrace()
             false
         }
    }
}