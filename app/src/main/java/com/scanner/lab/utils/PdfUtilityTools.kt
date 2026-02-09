package com.scanner.lab.utils

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.lowagie.text.Document
import com.lowagie.text.pdf.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

/**
 * Comprehensive PDF utility tools using OpenPDF (LGPL)
 * Fully Android Compatible (No AWT dependencies)
 */
object PdfUtilityTools {
    
    /**
     * Merge multiple PDFs
     */
    fun mergePdfs(
        inputPaths: List<String>,
        outputPath: String
    ): Result<File> = ErrorHandler.safe("MergePdfs") {
        val outputFile = File(outputPath)
        val document = Document()
        val copy = PdfCopy(document, FileOutputStream(outputFile))
        
        document.open()
        
        inputPaths.forEach { path ->
            val reader = PdfReader(path)
            val n = reader.numberOfPages
            for (i in 0 until n) {
                copy.addPage(copy.getImportedPage(reader, i + 1))
            }
        }
        
        document.close()
        outputFile
    }
    
    /**
     * Split PDF into individual pages
     */
    fun splitPdf(
        inputPath: String,
        outputDir: String,
        pageRanges: List<IntRange>? = null
    ): Result<List<File>> = ErrorHandler.safe("SplitPdf") {
        val outputDirectory = File(outputDir).apply { mkdirs() }
        val outputFiles = mutableListOf<File>()
        
        val reader = PdfReader(inputPath)
        val totalPages = reader.numberOfPages
        
        val ranges = pageRanges ?: listOf(0 until totalPages)
        
        ranges.forEachIndexed { index, range ->
            val outputFile = File(outputDirectory, "split_$index.pdf")
            val document = Document()
            val copy = PdfCopy(document, FileOutputStream(outputFile))
            
            document.open()
            
            range.forEach { pageNum ->
                // Convert 0-based index to 1-based for OpenPDF
                if (pageNum < totalPages) {
                    copy.addPage(copy.getImportedPage(reader, pageNum + 1))
                }
            }
            
            document.close()
            outputFiles.add(outputFile)
        }
        
        outputFiles
    }
    
    /**
     * Rotate PDF pages
     */
    fun rotatePdf(
        inputPath: String,
        outputPath: String,
        rotation: Int, // 90, 180, 270
        pageNumbers: List<Int>? = null
    ): Result<File> = ErrorHandler.safe("RotatePdf") {
        val inputFile = File(inputPath)
        val outputFile = File(outputPath)
        
        val reader = PdfReader(inputFile.absolutePath)
        val n = reader.numberOfPages
        val rot = rotation % 360
        
        val pagesToRotate = pageNumbers ?: (1..n).toList() // OpenPDF is 1-based
        
        // Setup dictionary
        pagesToRotate.forEach { pageNum ->
             if (pageNum <= n) {
                 val pageDict = reader.getPageN(pageNum)
                 val currentRotation = pageDict.getAsNumber(PdfName.ROTATE)?.intValue() ?: 0
                 pageDict.put(PdfName.ROTATE, PdfNumber((currentRotation + rot) % 360))
             }
        }
        
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        stamper.close()
        reader.close()
        
        outputFile
    }
    
    /**
     * Get PDF metadata
     */
    fun getPdfMetadata(inputPath: String): Result<PdfMetadata> = ErrorHandler.safe("GetPdfMetadata") {
        val reader = PdfReader(inputPath)
        @Suppress("UNCHECKED_CAST")
        val info = reader.info as HashMap<String, String>
        
        PdfMetadata(
            title = info["Title"] ?: "",
            author = info["Author"] ?: "",
            subject = info["Subject"] ?: "",
            pageCount = reader.numberOfPages,
            fileSize = File(inputPath).length()
        )
    }

    /**
     * Watermark PDF (Simple Text)
     */
    fun watermarkPdf(
        inputPath: String,
        outputPath: String,
        watermarkText: String
    ): Result<File> = ErrorHandler.safe("WatermarkPdf") {
        val reader = PdfReader(inputPath)
        val outputFile = File(outputPath)
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        val n = reader.numberOfPages
        
        for (i in 1..n) {
            val canvas = stamper.getOverContent(i)
            val font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
            canvas.beginText()
            canvas.setFontAndSize(font, 32f)
            canvas.showTextAligned(Element.ALIGN_CENTER, watermarkText, 300f, 400f, 45f)
            canvas.endText()
        }
        
        stamper.close()
        reader.close()
        outputFile
    }

    data class PdfMetadata(
        val title: String,
        val author: String,
        val subject: String,
        val pageCount: Int,
        val fileSize: Long
    )
}
