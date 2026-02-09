package com.scanner.lab.converters

import com.scanner.lab.utils.ErrorHandler
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File

/**
 * HTML web page converter
 */
object HtmlConverter {
    
    /**
     * HTML to PDF - extracts text content and creates PDF
     */
    fun htmlToPdf(
        htmlPath: String,
        outputPdfPath: String
    ): Result<File> = ErrorHandler.safe("HtmlToPdf") {
        val outputFile = File(outputPdfPath)
        val htmlFile = File(htmlPath)
        
        // Read HTML and strip tags to get plain text
        val htmlContent = htmlFile.readText()
        val textContent = htmlContent
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        // Use TextConverter to create PDF from extracted text
        TextConverter.textToPdf(textContent, outputPdfPath).getOrThrow()
        
        outputFile
    }
    
    /**
     * PDF to HTML (responsive web pages)
     */
    suspend fun pdfToHtml(
        pdfPath: String,
        outputHtmlPath: String
    ): Result<File> = ErrorHandler.safe("PdfToHtml") {
        val outputFile = File(outputHtmlPath)
        
        // Extract text from PDF
        val text = TextConverter.pdfToText(pdfPath).getOrThrow()
        
        // Create responsive HTML
        val html = createResponsiveHtml(text, "Converted Document")
        
        outputFile.writeText(html)
        
        outputFile
    }
    
    /**
     * Text to HTML with premium templates
     */
    fun textToHtml(
        text: String,
        outputHtmlPath: String,
        title: String = "Document"
    ): Result<File> = ErrorHandler.safe("TextToHtml") {
        val outputFile = File(outputHtmlPath)
        
        val html = createResponsiveHtml(text, title)
        
        outputFile.writeText(html)
        
        outputFile
    }
    
    /**
     * Create responsive HTML with premium styling
     */
    private fun createResponsiveHtml(content: String, title: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$title</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #0F172A;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 800px;
            margin: 0 auto;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-radius: 16px;
            padding: 40px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }
        
        h1 {
            color: #6366F1;
            margin-bottom: 20px;
            font-size: 2.5em;
            background: linear-gradient(135deg, #6366F1, #EC4899);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        
        p {
            margin-bottom: 15px;
            color: #334155;
        }
        
        @media (max-width: 768px) {
            .container {
                padding: 20px;
            }
            
            h1 {
                font-size: 1.8em;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>$title</h1>
        ${content.split("\n").joinToString("") { "<p>${it.trim()}</p>" }}
    </div>
</body>
</html>
        """.trimIndent()
    }
}
