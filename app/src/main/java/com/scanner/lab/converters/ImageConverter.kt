package com.scanner.lab.converters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.scanner.lab.utils.ErrorHandler
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Enhanced image converter with multiple format support
 */
object ImageConverter {
    
    /**
     * Simple bitmap recycler utility
     */
    private fun recycleBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
    
    enum class ImageFormat {
        PNG, JPG, JPEG, WEBP
    }
    
    /**
     * PDF to Images (PNG, JPG, WebP with quality control)
     */
    fun pdfToImages(
        pdfPath: String,
        outputDir: String,
        format: ImageFormat = ImageFormat.PNG,
        quality: Int = 85
    ): Result<List<File>> = ErrorHandler.safe("PdfToImages") {
        val outputDirectory = File(outputDir).apply { mkdirs() }
        val imageFiles = mutableListOf<File>()
        
        val pdfFile = File(pdfPath)
        val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)
        
        for (i in 0 until pdfRenderer.pageCount) {
            pdfRenderer.openPage(i).use { page ->
                // High quality rendering
                val scale = 2.0f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val extension = when (format) {
                    ImageFormat.PNG -> "png"
                    ImageFormat.JPG, ImageFormat.JPEG -> "jpg"
                    ImageFormat.WEBP -> "webp"
                }
                
                val imageFile = File(outputDirectory, "page_$i.$extension")
                
                FileOutputStream(imageFile).use { out ->
                    val compressFormat = when (format) {
                        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                        ImageFormat.JPG, ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                        ImageFormat.WEBP -> Bitmap.CompressFormat.WEBP
                    }
                    
                    bitmap.compress(compressFormat, quality, out)
                }
                
                recycleBitmap(bitmap)
                imageFiles.add(imageFile)
            }
        }
        
        pdfRenderer.close()
        parcelFileDescriptor.close()
        
        imageFiles
    }
    
    /**
     * Images to PDF (batch conversion with compression)
     */
    fun imagesToPdf(
        imagePaths: List<String>,
        outputPdfPath: String,
        compress: Boolean = true
    ): Result<File> = ErrorHandler.safe("ImagesToPdf") {
        val outputFile = File(outputPdfPath)
        
        PDDocument().use { document ->
            imagePaths.forEach { imagePath ->
                val bitmap = BitmapFactory.decodeFile(imagePath)
                
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                
                PDPageContentStream(document, page).use { contentStream ->
                    // Convert Android Bitmap to byte array for PDFBox
                    val baos = ByteArrayOutputStream()
                    val quality = if (compress) 80 else 100
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    val imageBytes = baos.toByteArray()
                    
                    // Create PDImageXObject from bytes
                    val pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "image")
                    
                    // Scale image to fit page
                    val pageWidth = page.mediaBox.width
                    val pageHeight = page.mediaBox.height
                    val imageWidth = bitmap.width.toFloat()
                    val imageHeight = bitmap.height.toFloat()
                    
                    val scale = minOf(pageWidth / imageWidth, pageHeight / imageHeight)
                    val scaledWidth = imageWidth * scale
                    val scaledHeight = imageHeight * scale
                    
                    val x = (pageWidth - scaledWidth) / 2
                    val y = (pageHeight - scaledHeight) / 2
                    
                    contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight)
                }
                
                recycleBitmap(bitmap)
            }
            
            document.save(outputFile)
        }
        
        outputFile
    }
    
    /**
     * Convert image format (PNG ↔ JPG ↔ WebP)
     */
    fun convertImageFormat(
        inputPath: String,
        outputPath: String,
        targetFormat: ImageFormat,
        quality: Int = 85
    ): Result<File> = ErrorHandler.safe("ConvertImageFormat") {
        val outputFile = File(outputPath)
        
        val bitmap = BitmapFactory.decodeFile(inputPath)
        
        FileOutputStream(outputFile).use { out ->
            val compressFormat = when (targetFormat) {
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageFormat.JPG, ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ImageFormat.WEBP -> Bitmap.CompressFormat.WEBP
            }
            
            bitmap.compress(compressFormat, quality, out)
        }
        
        recycleBitmap(bitmap)
        
        outputFile
    }
    
    /**
     * Optimize image (auto-compress while maintaining quality)
     */
    fun optimizeImage(
        inputPath: String,
        outputPath: String,
        targetSizeKb: Int = 500
    ): Result<File> = ErrorHandler.safe("OptimizeImage") {
        val outputFile = File(outputPath)
        var bitmap = BitmapFactory.decodeFile(inputPath)
        
        // Check if downsampling is needed
        val originalSize = File(inputPath).length() / 1024
        
        if (originalSize > targetSizeKb * 2) {
            // Downsample if significantly larger
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            bitmap = BitmapFactory.decodeFile(inputPath, options)
        }
        
        // Try different quality levels
        var quality = 85
        var success = false
        
        while (quality >= 60 && !success) {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            val resultSize = outputFile.length() / 1024
            if (resultSize <= targetSizeKb) {
                success = true
            } else {
                quality -= 5
            }
        }
        
        recycleBitmap(bitmap)
        
        outputFile
    }
}
