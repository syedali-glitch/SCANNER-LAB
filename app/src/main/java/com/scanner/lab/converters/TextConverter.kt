package com.scanner.lab.converters

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scanner.lab.utils.ErrorHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Text converter with OCR support
 */
object TextConverter {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * PDF to Text (OCR-based extraction)
     */
    suspend fun pdfToText(pdfPath: String): Result<String> = ErrorHandler.safe("PdfToText") {
        val pdfFile = File(pdfPath)
        val textBuilder = StringBuilder()
        
        val parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)
        
        for (i in 0 until pdfRenderer.pageCount) {
            pdfRenderer.openPage(i).use { page ->
                val bitmap = android.graphics.Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val text = recognizeText(bitmap)
                textBuilder.append("--- Page ${i + 1} ---\n")
                textBuilder.append(text)
                textBuilder.append("\n\n")
                
                bitmap.recycle()
            }
        }
        
        pdfRenderer.close()
        parcelFileDescriptor.close()
        
        textBuilder.toString()
    }
    
    /**
     * Text to PDF (formatted with proper paragraphs)
     */
    fun textToPdf(
        textContent: String,
        outputPdfPath: String
    ): Result<File> = ErrorHandler.safe("TextToPdf") {
        val outputFile = File(outputPdfPath)
        
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            
            PDPageContentStream(document, page).use { contentStream ->
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 12f)
                contentStream.setLeading(14.5f)
                contentStream.newLineAtOffset(50f, page.mediaBox.height - 50)
                
                val lines = wrapText(textContent, 80)
                var currentY = page.mediaBox.height - 50
                
                lines.forEach { line ->
                    if (currentY < 50) {
                        // New page needed
                        contentStream.endText()
                        contentStream.close()
                        
                        val newPage = PDPage(PDRectangle.A4)
                        document.addPage(newPage)
                        
                        PDPageContentStream(document, newPage).use { newContentStream ->
                            newContentStream.beginText()
                            newContentStream.setFont(PDType1Font.HELVETICA, 12f)
                            newContentStream.setLeading(14.5f)
                            newContentStream.newLineAtOffset(50f, newPage.mediaBox.height - 50)
                            newContentStream.showText(line)
                            newContentStream.newLine()
                            newContentStream.endText()
                        }
                        currentY = page.mediaBox.height - 50
                    } else {
                        contentStream.showText(line)
                        contentStream.newLine()
                        currentY -= 14.5f
                    }
                }
                
                contentStream.endText()
            }
            
            document.save(outputFile)
        }
        
        outputFile
    }
    
    /**
     * Read text file with encoding detection
     */
    fun readTextFile(filePath: String): Result<String> = ErrorHandler.safe("ReadTextFile") {
        val file = File(filePath)
        
        // Try different encodings
        val encodings = listOf(Charsets.UTF_8, Charsets.UTF_16, Charsets.ISO_8859_1)
        
        var content = ""
        for (encoding in encodings) {
            try {
                content = file.readText(encoding)
                if (content.isNotBlank()) break
            } catch (e: Exception) {
                // Try next encoding
            }
        }
        
        content
    }
    
    /**
     * Recognize text from bitmap using ML Kit
     */
    private suspend fun recognizeText(bitmap: android.graphics.Bitmap): String = 
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    
    /**
     * Wrap text to specified width
     */
    private fun wrapText(text: String, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()
        
        words.forEach { word ->
            if (currentLine.length + word.length + 1 > maxWidth) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines
    }
}
