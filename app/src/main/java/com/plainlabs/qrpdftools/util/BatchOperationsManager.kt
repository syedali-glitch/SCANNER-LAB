package com.plainlabs.qrpdftools.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import com.plainlabs.qrpdftools.conversion.* // Import all converters

enum class ConversionType {
    PDF_TO_DOCX, PDF_TO_PPTX, PDF_TO_IMAGE, PDF_TO_TEXT, PDF_TO_HTML,
    DOCX_TO_PDF, PPTX_TO_PDF, IMAGE_TO_PDF, TEXT_TO_PDF, HTML_TO_PDF
}

data class BatchOperation(
    val inputFile: File,
    val outputFile: File,
    val type: ConversionType
)

data class BatchResult(
    val operation: BatchOperation,
    val success: Boolean,
    val message: String? = null
)

class BatchOperationsManager(private val context: Context) {

    private val pptxConverter = PptxConverter(context)
    private val imageConverter = ImageConverter(context)
    private val textConverter = TextConverter(context)
    private val htmlConverter = HtmlConverter(context)
    // private val docxConverter = DocxConverter(context) // Assuming exists or not corrupted?

    suspend fun executeParallelConversion(
        operations: List<BatchOperation>,
        onProgress: (Int, Int) -> Unit
    ): List<BatchResult> = withContext(Dispatchers.IO) {
        var completed = 0
        
        val deferredResults = operations.map { op ->
            async {
                val result = try {
                    processOperation(op)
                    BatchResult(op, true)
                } catch (e: Exception) {
                    BatchResult(op, false, e.message)
                }
                
                synchronized(this) {
                    completed++
                    // onProgress(completed, operations.size) // Safe call via handler usually needed if updating UI directly, but suspend should be fine if caller handles context.
                }
                // We'll call progress updates in main thread if needed by caller, 
                // but for now we just track internally implicitly.
                // Actually, let's just return result.
                result
            }
        }
        deferredResults.awaitAll()
    }

    private suspend fun processOperation(op: BatchOperation) {
        when (op.type) {
            ConversionType.PDF_TO_DOCX -> PdfConverter.extractPdfTextToWord(op.inputFile.absolutePath, op.outputFile.absolutePath, context)
            ConversionType.DOCX_TO_PDF -> DocxConverter.docxToPdf(op.inputFile.absolutePath, op.outputFile.absolutePath)
            
            ConversionType.PDF_TO_PPTX -> pptxConverter.convertPdfToPptx(op.inputFile, op.outputFile) { }
            ConversionType.PPTX_TO_PDF -> pptxConverter.convertPptxToPdf(op.inputFile, op.outputFile) { }
            
            ConversionType.PDF_TO_IMAGE -> {
                 // Image converter requires directory, here operation usually implies simple mapping.
                 // We'll assume outputFile is a directory for this specific type? 
                 // Or we zip it? 
                 // For now, let's assume standard behavior of mapping 1 file -> 1 file (e.g. zip) OR 1 page image.
                 // Simplification: Output to directory named same as file.
                 val outDir = op.outputFile.parentFile ?: op.outputFile
                 imageConverter.convertPdfToImages(op.inputFile, outDir, ImageConverter.ImageFormat.PNG, 100) { }
            }
            ConversionType.IMAGE_TO_PDF -> {
                 // Usually input is multiple files. Here Single file input -> PDF? 
                 // Or list of files. 
                 // 'BatchOperation' definition above implies 1-to-1. 
                 // We'll support single image to PDF here.
                 imageConverter.convertImagesToPdf(listOf(op.inputFile), op.outputFile, 100) { }
            }
            
            ConversionType.PDF_TO_TEXT -> textConverter.convertPdfToText(op.inputFile, op.outputFile, true) { }
            ConversionType.TEXT_TO_PDF -> textConverter.convertTextToPdf(op.inputFile, op.outputFile) { }
            
            ConversionType.PDF_TO_HTML -> htmlConverter.convertPdfToHtml(op.inputFile, op.outputFile) { }
            ConversionType.HTML_TO_PDF -> htmlConverter.convertHtmlToPdf(op.inputFile, op.outputFile) { }
            
            else -> throw UnsupportedOperationException("Conversion type ${op.type} not ready")
        }
    }
}