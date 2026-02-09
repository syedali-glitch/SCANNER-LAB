package com.scanner.lab.batch

import com.scanner.lab.converters.*
import com.scanner.lab.performance.OptimizedConversionEngine
import com.scanner.lab.utils.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Batch operations manager with parallel processing
 */
class BatchOperationsManager {
    
    private val conversionEngine = OptimizedConversionEngine()
    
    enum class ConversionType {
        PDF_TO_DOCX,
        PDF_TO_PPTX,
        PDF_TO_IMAGE,
        PDF_TO_TEXT,
        PDF_TO_HTML,
        DOCX_TO_PDF,
        PPTX_TO_PDF,
        IMAGE_TO_PDF,
        TEXT_TO_PDF,
        HTML_TO_PDF
    }
    
    data class BatchOperation(
        val inputFile: File,
        val outputFile: File,
        val conversionType: ConversionType
    )
    
    data class BatchResult(
        val operation: BatchOperation,
        val success: Boolean,
        val error: String? = null
    )
    
    /**
     * Progress callback interface
     */
    interface BatchProgressCallback {
        fun onProgress(current: Int, total: Int)
        fun onOperationComplete(result: BatchResult)
        fun onAllComplete(results: List<BatchResult>)
    }
    
    /**
     * Execute batch conversion sequentially
     */
    suspend fun executeSequentialConversion(
        operations: List<BatchOperation>,
        callback: BatchProgressCallback? = null
    ): List<BatchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<BatchResult>()
        
        operations.forEachIndexed { index, operation ->
            val result = performConversion(operation)
            results.add(result)
            
            withContext(Dispatchers.Main) {
                callback?.onProgress(index + 1, operations.size)
                callback?.onOperationComplete(result)
            }
        }
        
        withContext(Dispatchers.Main) {
            callback?.onAllComplete(results)
        }
        
        results
    }
    
    /**
     * Execute batch conversion in parallel (up to 4 files simultaneously)
     */
    suspend fun executeParallelConversion(
        operations: List<BatchOperation>,
        callback: BatchProgressCallback? = null
    ): List<BatchResult> = withContext(Dispatchers.IO) {
        val total = operations.size
        var completed = 0
        val results = mutableListOf<BatchResult>()
        
        val conversionTasks = operations.map { operation ->
            suspend {
                performConversion(operation).also { result ->
                    synchronized(results) {
                        results.add(result)
                        completed++
                    }
                    
                    withContext(Dispatchers.Main) {
                        callback?.onProgress(completed, total)
                        callback?.onOperationComplete(result)
                    }
                }
            }
        }
        
        // Execute with parallel processing
        conversionEngine.parallelConversion(
            operations.map { it.inputFile },
            { file ->
                val operation = operations.find { it.inputFile == file }!!
                performConversion(operation)
                Result.success(operation.outputFile)
            }
        )
        
        withContext(Dispatchers.Main) {
            callback?.onAllComplete(results)
        }
        
        results
    }
    
    /**
     * Perform single conversion
     */
    private suspend fun performConversion(operation: BatchOperation): BatchResult {
        return try {
            when (operation.conversionType) {
                ConversionType.PDF_TO_DOCX -> {
                    DocxConverter.pdfToDocx(
                        operation.inputFile.absolutePath,
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
                
                ConversionType.PDF_TO_PPTX -> {
                    PptxConverter.pdfToPptx(
                        operation.inputFile.absolutePath,
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
                
                ConversionType.PDF_TO_IMAGE -> {
                    ImageConverter.pdfToImages(
                        operation.inputFile.absolutePath,
                        operation.outputFile.parent!!,
                        ImageConverter.ImageFormat.PNG
                    ).getOrThrow()
                }
                
                ConversionType.PDF_TO_TEXT -> {
                    val text = TextConverter.pdfToText(
                        operation.inputFile.absolutePath
                    ).getOrThrow()
                    operation.outputFile.writeText(text)
                }
                
                ConversionType.PDF_TO_HTML -> {
                    HtmlConverter.pdfToHtml(
                        operation.inputFile.absolutePath,
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
                
                ConversionType.DOCX_TO_PDF -> {
                    DocxConverter.docxToPdf(
                        operation.inputFile.absolutePath,
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
                
                ConversionType.PPTX_TO_PDF -> {
                    PptxConverter.pptxToPdf(
                        operation.inputFile.absolutePath,
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
                
                ConversionType.IMAGE_TO_PDF -> {
                    ImageConverter.imagesToPdf(
                        listOf(operation.inputFile.absolutePath),
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
                
                ConversionType.TEXT_TO_PDF -> {
                    val text = TextConverter.readTextFile(
                        operation.inputFile.absolutePath
                    ).getOrThrow()
                    TextConverter.textToPdf(text, operation.outputFile.absolutePath).getOrThrow()
                }
                
                ConversionType.HTML_TO_PDF -> {
                    HtmlConverter.htmlToPdf(
                        operation.inputFile.absolutePath,
                        operation.outputFile.absolutePath
                    ).getOrThrow()
                }
            }
            
            BatchResult(operation, success = true)
        } catch (e: Exception) {
            BatchResult(operation, success = false, error = e.message)
        }
    }
}
