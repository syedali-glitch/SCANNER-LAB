package com.plainlabs.qrpdftools.conversion

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.IOException

/**
 * HtmlConverter - Refactored to Proprietary PlainLabs engine.
 * 
 * Uses Native Android WebView printing to generate PDFs from HTML.
 * Mandatory Architect Rule: WebView must be instantiated on the Main Thread.
 */
class HtmlConverter(private val context: Context) {

    /**
     * PDF to HTML - Text extraction wrapper.
     */
    fun convertPdfToHtml(pdfFile: File, outputHtml: File, callback: (Float) -> Unit) {
        try {
             val reader = com.lowagie.text.pdf.PdfReader(pdfFile.absolutePath)
             val extractor = com.lowagie.text.pdf.parser.PdfTextExtractor(reader)
             val sb = StringBuilder()
             for (i in 1..reader.numberOfPages) {
                 val text = extractor.getTextFromPage(i)
                 sb.append(text).append("\n\n")
             }
             reader.close()
             
             val text = sb.toString()
             // Create Premium HTML Template
             val htmlContent = """
                 <!DOCTYPE html>
                 <html>
                 <head>
                     <meta name="viewport" content="width=device-width, initial-scale=1.0">
                     <style>
                         body { font-family: sans-serif; line-height: 1.6; padding: 20px; background-color: #121212; color: #FFFFFF; }
                         .container { max-width: 800px; margin: 0 auto; background: #181818; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.5); }
                         p { margin-bottom: 1em; }
                     </style>
                 </head>
                 <body>
                     <div class="container">
                         ${text.split("\n\n").joinToString("") { "<p>${it.replace("\n", "<br>")}</p>" }}
                     </div>
                 </body>
                 </html>
             """.trimIndent()
             
             outputHtml.writeText(htmlContent)
             callback(1.0f)
             
        } catch (e: Exception) {
            throw IOException("PDF to HTML failed: ${e.message}")
        }
    }

    /**
     * HTML to PDF - Robust PdfDocument Implementation.
     * Uses WebView to render and draws it to a PDF canvas.
     */
    fun convertHtmlToPdf(htmlFile: File, outputPdf: File, callback: (Float) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(context)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Allow some time for rendering
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val width = 595 // A4 standard width in points
                                val height = (webView.contentHeight * context.resources.displayMetrics.density).toInt().coerceAtLeast(842)
                                
                                webView.measure(
                                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                                )
                                webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)
                                
                                val pdfDocument = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(webView.measuredWidth, webView.measuredHeight, 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                
                                webView.draw(page.canvas)
                                pdfDocument.finishPage(page)
                                
                                FileOutputStream(outputPdf).use { out ->
                                    pdfDocument.writeTo(out)
                                }
                                pdfDocument.close()
                                webView.destroy()
                                callback(1.0f)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(0f)
                            }
                        }, 500)
                    }
                }
                webView.loadUrl("file://${htmlFile.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
                callback(0f)
            }
        }
    }

    fun convertTextToHtml(textFile: File, outputHtml: File) {
         try {
             val text = textFile.readText()
             val htmlContent = """
                 <!DOCTYPE html>
                 <html>
                 <head>
                     <meta name="viewport" content="width=device-width, initial-scale=1.0">
                     <style>
                         body { font-family: sans-serif; padding: 20px; background: #121212; color: #FFFFFF; }
                         .content { background: #181818; padding: 20px; border-radius: 5px; }
                     </style>
                 </head>
                 <body>
                     <div class="content">
                         ${text.replace("\n", "<br>")}
                     </div>
                 </body>
                 </html>
             """.trimIndent()
             outputHtml.writeText(htmlContent)
         } catch (e: Exception) {
             throw IOException("Text to HTML failed: ${e.message}")
         }
    }
}