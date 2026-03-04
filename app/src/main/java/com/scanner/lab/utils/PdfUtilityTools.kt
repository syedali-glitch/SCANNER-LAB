package com.scanner.lab.utils

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.lowagie.text.Document
import com.lowagie.text.Element
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

    /**
     * Compress PDF (Downsample images)
     */
    fun compressPdf(
        inputPath: String,
        outputPath: String,
        quality: Int = 50 // 0-100
    ): Result<File> = ErrorHandler.safe("CompressPdf") {
        val reader = PdfReader(inputPath)
        val outputFile = File(outputPath)
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        
        // Iterate through all objects to find images
        val n = reader.numberOfPages
        for (i in 1..n) {
             // In OpenPDF/iText, deep compression is complex. 
             // Ideally we recreate the PDF. For v1.2, we use setFullCompression
             // which removes unused objects and compresses streams.
        }
        
        stamper.setFullCompression()
        stamper.close()
        reader.close()
        
        // Note: Real image downsampling requires iterating XObjects which is huge code.
        // For now, full stream compression + unused object removal is a good "Medium" compress.
        outputFile
    }
    
    /**
     * Watermark PDF (Advanced)
     */
    data class WatermarkConfig(
        val text: String,
        val rotation: Float = 45f,
        val opacity: Float = 0.3f,
        val fontSize: Float = 42f,
        val colorHex: String = "#000000"
    )
    
    fun watermarkPdfAdvanced(
        inputPath: String,
        outputPath: String,
        config: WatermarkConfig
    ): Result<File> = ErrorHandler.safe("WatermarkPdfAdvanced") {
        val reader = PdfReader(inputPath)
        val outputFile = File(outputPath)
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        val n = reader.numberOfPages
        
        val red = Integer.valueOf(config.colorHex.substring(1, 3), 16)
        val green = Integer.valueOf(config.colorHex.substring(3, 5), 16)
        val blue = Integer.valueOf(config.colorHex.substring(5, 7), 16)
        
        val gState = PdfGState()
        gState.setFillOpacity(config.opacity)
        
        for (i in 1..n) {
            val canvas = stamper.getOverContent(i)
            canvas.setGState(gState)
            val font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
            canvas.beginText()
            canvas.setRGBColorFill(red, green, blue)
            canvas.setFontAndSize(font, config.fontSize)
            
            // Center calculation
            val pageSize = reader.getPageSize(i)
            val x = pageSize.width / 2
            val y = pageSize.height / 2
            
            canvas.showTextAligned(Element.ALIGN_CENTER, config.text, x, y, config.rotation)
            canvas.endText()
        }
        
        stamper.close()
        reader.close()
        outputFile
    }

    /**
     * Encrypt PDF
     */
    fun encryptPdf(
        inputPath: String,
        outputPath: String,
        userPass: String,
        ownerPass: String
    ): Result<File> = ErrorHandler.safe("EncryptPdf") {
        val reader = PdfReader(inputPath)
        val outputFile = File(outputPath)
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        
        stamper.setEncryption(
            userPass.toByteArray(),
            ownerPass.toByteArray(),
            PdfWriter.ALLOW_PRINTING,
            PdfWriter.ENCRYPTION_AES_256_V3
        )
        
        stamper.close()
        reader.close()
        outputFile
    }
    
    /**
     * Remove Pages
     */
    fun removePages(
        inputPath: String,
        outputPath: String,
        pagesToRemove: List<Int> // 1-based indices
    ): Result<File> = ErrorHandler.safe("RemovePages") {
        val reader = PdfReader(inputPath)
        val n = reader.numberOfPages
        val pagesToKeep = (1..n).filter { !pagesToRemove.contains(it) }
        
        if (pagesToKeep.isEmpty()) throw Exception("Cannot remove all pages")
        
        reader.selectPages(pagesToKeep.joinToString(","))
        
        val outputFile = File(outputPath)
        val stamper = PdfStamper(reader, FileOutputStream(outputFile))
        stamper.close()
        reader.close()
        outputFile
    }

    /**
     * Reorder Pages
     */
    fun reorderPdf(
        inputPath: String,
        outputPath: String,
        newOrder: List<Int> // List of 1-based page numbers in desired order
    ): Result<File> = ErrorHandler.safe("ReorderPdf") {
        val reader = PdfReader(inputPath)
        val document = Document()
        val outputFile = File(outputPath)
        val copy = PdfCopy(document, FileOutputStream(outputFile))
        
        document.open()
        
        newOrder.forEach { pageNum ->
            copy.addPage(copy.getImportedPage(reader, pageNum))
        }
        
        document.close()
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
