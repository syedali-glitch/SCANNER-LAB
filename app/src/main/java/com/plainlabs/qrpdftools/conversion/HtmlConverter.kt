package com.plainlabs.qrpdftools.conversion

import android.content.Context
import android.os.Handler
import android.os.Looper
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
                 sb.append(extractor.getTextFromPage(i)).append("\n\n")
             }
             reader.close()
             
             val text = sb.toString()
             // Create Premium HTML Template (Same as before)
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
     * HTML to PDF - Native WebView Print Implementation.
     * Architect Rule: UI Thread for instantiation.
     */
    fun convertHtmlToPdf(htmlFile: File, outputPdf: File, callback: (Float) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(context)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()

                        val adapter = webView.createPrintDocumentAdapter("PlainLabs_Scan")
                        
                        // Headless Print Execution
                        val descriptor = android.os.ParcelFileDescriptor.open(outputPdf, android.os.ParcelFileDescriptor.MODE_READ_WRITE or android.os.ParcelFileDescriptor.MODE_CREATE or android.os.ParcelFileDescriptor.MODE_TRUNCATE)
                        
                        adapter.onLayout(null, printAttributes, null, object : PrintDocumentAdapter.LayoutResultCallback() {
                            override fun onLayoutFinished(info: android.print.PrintDocumentInfo?, changed: Boolean) {
                                adapter.onWrite(arrayOf(android.print.PageRange.ALL_PAGES), descriptor, null, object : PrintDocumentAdapter.WriteResultCallback() {
                                    override fun onWriteFinished(pages: Array<out android.print.PageRange>?) {
                                        try {
                                            descriptor.close()
                                            webView.destroy()
                                            callback(1.0f)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    override fun onWriteFailed(error: CharSequence?) {
                                        try { descriptor.close() } catch (e: Exception) {}
                                        webView.destroy()
                                    }
                                })
                            }
                        }, null)
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