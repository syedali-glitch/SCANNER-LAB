package com.scanner.lab.converters

import com.scanner.lab.utils.ErrorHandler
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Bidirectional PowerPoint converter
 */
object PptxConverter {
    
    /**
     * PPTX to PDF
     */
    fun pptxToPdf(
        pptxPath: String,
        outputPdfPath: String
    ): Result<File> = ErrorHandler.safe("PptxToPdf") {
        val outputFile = File(outputPdfPath)
        val pptxFile = File(pptxPath)
        
        FileInputStream(pptxFile).use { fis ->
            val pptx = XMLSlideShow(fis)
            
            PDDocument().use { pdfDoc ->
                pptx.slides.forEach { slide ->
                    val page = PDPage(PDRectangle.A4)
                    pdfDoc.addPage(page)
                    
                    PDPageContentStream(pdfDoc, page).use { contentStream ->
                        contentStream.beginText()
                        contentStream.setFont(PDType1Font.HELVETICA, 12f)
                        contentStream.setLeading(14.5f)
                        contentStream.newLineAtOffset(50f, page.mediaBox.height - 50)
                        
                        // Extract text from shapes
                        slide.shapes.forEach { shape ->
                            if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                                val text = shape.text
                                if (text.isNotBlank()) {
                                    contentStream.showText(text)
                                    contentStream.newLine()
                                }
                            }
                        }
                        
                        contentStream.endText()
                    }
                }
                
                pdfDoc.save(outputFile)
            }
            
            pptx.close()
        }
        
        outputFile
    }
    
    /**
     * PDF to PPTX (each page becomes a slide)
     */
    suspend fun pdfToPptx(
        pdfPath: String,
        outputPptxPath: String
    ): Result<File> = ErrorHandler.safe("PdfToPptx") {
        val outputFile = File(outputPptxPath)
        
        // Extract text from PDF
        val text = TextConverter.pdfToText(pdfPath).getOrThrow()
        
        // Create PPTX with text
        textToPptx(text, outputPptxPath).getOrThrow()
        
        outputFile
    }
    
    /**
     * Text to PPTX (auto-formatted presentations)
     */
    fun textToPptx(
        text: String,
        outputPptxPath: String
    ): Result<File> = ErrorHandler.safe("TextToPptx") {
        val outputFile = File(outputPptxPath)
        
        XMLSlideShow().use { pptx ->
            // Split text into slides (each paragraph or section)
            val slides = text.split("\n\n").filter { it.isNotBlank() }
            
            slides.forEach { slideText ->
                val slide = pptx.createSlide()
                
                // Create text box with simple text content (no positioning - AWT not available on Android)
                val textBox = slide.createTextBox()
                
                // Add title text
                val titleParagraph = textBox.addNewTextParagraph()
                val titleRun = titleParagraph.addNewTextRun()
                titleRun.setText(slideText.take(50))
                titleRun.fontSize = 32.0
                titleRun.isBold = true
                
                // Add body content if there's more text
                if (slideText.length > 50) {
                    val contentParagraph = textBox.addNewTextParagraph()
                    val contentRun = contentParagraph.addNewTextRun()
                    contentRun.setText(slideText.substring(50))
                    contentRun.fontSize = 18.0
                }
            }
            
            FileOutputStream(outputFile).use { fos ->
                pptx.write(fos)
            }
        }
        
        outputFile
    }
    
    /**
     * Images to PPTX (Embed images, one per slide)
     */
    fun imagesToPptx(
        imagePaths: List<String>,
        outputPptxPath: String
    ): Result<File> = ErrorHandler.safe("ImagesToPptx") {
        val outputFile = File(outputPptxPath)
        
        XMLSlideShow().use { pptx ->
            
            imagePaths.forEach { imagePath ->
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    val slide = pptx.createSlide()
                    
                    val pictureIndex = FileInputStream(imageFile).use { fis ->
                         val format = if (imagePath.endsWith("png", true)) {
                            org.apache.poi.sl.usermodel.PictureData.PictureType.PNG
                        } else {
                            org.apache.poi.sl.usermodel.PictureData.PictureType.JPEG
                        }
                        pptx.addPicture(fis, format)
                    }
                    
                    val pictureShape = slide.createPicture(pictureIndex)
                    // Center Picture: 720 x 540 default.
                    // Let's make it fill most of the slide or center.
                    // pictureShape.anchor = java.awt.Rectangle(40, 40, 640, 460) // AWT not available on Android
                }
            }
            
            FileOutputStream(outputFile).use { fos ->
                pptx.write(fos)
            }
        }
        
        outputFile
    }

    /**
     * PPTX to Text (extract all slide content)
     */
    fun pptxToText(pptxPath: String): Result<String> = ErrorHandler.safe("PptxToText") {
        val pptxFile = File(pptxPath)
        val textBuilder = StringBuilder()
        
        FileInputStream(pptxFile).use { fis ->
            val pptx = XMLSlideShow(fis)
            
            pptx.slides.forEachIndexed { index, slide ->
                textBuilder.append("--- Slide ${index + 1} ---\n")
                
                slide.shapes.forEach { shape ->
                    if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                        textBuilder.append(shape.text)
                        textBuilder.append("\n")
                    }
                }
                
                textBuilder.append("\n")
            }
            
            pptx.close()
        }
        
        textBuilder.toString()
    }
}
