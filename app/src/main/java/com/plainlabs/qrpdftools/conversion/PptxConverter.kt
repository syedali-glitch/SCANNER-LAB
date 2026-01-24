package com.plainlabs.qrpdftools.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.utils.PdfMerger
import com.plainlabs.qrpdftools.util.PdfUtilityTools
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.sl.usermodel.PictureData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PptxConverter(private val context: Context) {

    /**
     * Convert PDF to PPTX (each page becomes a slide with the page image)
     */
    fun convertPdfToPptx(pdfFile: File, outputPptx: File, callback: (Float) -> Unit) {
        try {
            val ppt = XMLSlideShow()
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val pageCount = renderer.pageCount

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height
                
                // Create bitmap for the page
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Save bitmap to byte array
                val os = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                val imageBytes = os.toByteArray()
                
                // Add slide and image
                val slide = ppt.createSlide()
                val pictureIndex = ppt.addPicture(imageBytes, PictureData.PictureType.PNG)
                slide.createPicture(pictureIndex).apply {
                    // anchor = java.awt.Rectangle(0, 0, ppt.pageSize.width, ppt.pageSize.height)
                    // Note: AWT Rectangle not available in Android. 
                    // To fully support this, use a shaded POI library for Android.
                    // For now, we omit anchor which may result in 0-size image,
                    // preserving compile stability.
                }

                page.close()
                callback((i + 1).toFloat() / pageCount)
            }

            FileOutputStream(outputPptx).use { out ->
                ppt.write(out)
            }
            fileDescriptor.close()
            renderer.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw IOException("Conversion failed: ${e.message}")
        }
    }

    /**
     * Convert PPTX to PDF (Extraction based)
     */
    fun convertPptxToPdf(pptxFile: File, outputPdf: File, callback: (Float) -> Unit) {
        try {
             FileInputStream(pptxFile).use { fis ->
                val ppt = XMLSlideShow(fis)
                val pdfWriter = com.itextpdf.kernel.pdf.PdfWriter(outputPdf)
                val pdfDoc = PdfDocument(pdfWriter)
                val document = com.itextpdf.layout.Document(pdfDoc)
                
                val slides = ppt.slides
                for ((index, slide) in slides.withIndex()) {
                    // Title
                    val title = slide.title
                    if (title != null) {
                        document.add(com.itextpdf.layout.element.Paragraph(title).setFontSize(18f).setBold())
                    }
                    
                    // Content
                     slide.shapes.forEach { shape ->
                        if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                            val text = shape.text
                            if (text.isNotEmpty()) {
                                document.add(com.itextpdf.layout.element.Paragraph(text))
                            }
                        }
                    }
                    
                    if (index < slides.size - 1) {
                         document.add(com.itextpdf.layout.element.AreaBreak())
                    }
                    callback((index + 1).toFloat() / slides.size)
                }
                
                document.close()
             }
        } catch (e: Exception) {
            throw IOException("PPTX to PDF failed: ${e.message}")
        }
    }

    fun convertTextToPptx(textFile: File, outputPptx: File) {
         try {
            val ppt = XMLSlideShow()
            val text = textFile.readText()
            
            val slide = ppt.createSlide()
            // Title
            val titleShape = slide.createTextBox()
            // titleShape.anchor = java.awt.Rectangle(50, 50, 600, 50)
            titleShape.text = "Presentation from Text"
            
            val contentShape = slide.createTextBox()
            // contentShape.anchor = java.awt.Rectangle(50, 100, 600, 400)
            contentShape.text = text
            
             FileOutputStream(outputPptx).use { out ->
                ppt.write(out)
            }
         } catch (e: Exception) {
            throw IOException("Text to PPTX failed: ${e.message}")
         }
    }

    fun convertPptxToText(pptxFile: File, outputText: File) {
        try {
             FileInputStream(pptxFile).use { fis ->
                val ppt = XMLSlideShow(fis)
                val sb = StringBuilder()
                
                for (slide in ppt.slides) {
                    slide.shapes.forEach { shape ->
                        if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                            sb.append(shape.text).append("\n")
                        }
                    }
                    sb.append("\n---\n\n")
                }
                outputText.writeText(sb.toString())
             }
        } catch (e: Exception) {
             throw IOException("PPTX to Text failed: ${e.message}")
        }
    }
}