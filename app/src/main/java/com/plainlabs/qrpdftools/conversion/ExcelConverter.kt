package com.plainlabs.qrpdftools.conversion

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.lowagie.text.Document
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * ExcelConverter - Refactored to use OpenPDF.
 */
object ExcelConverter {

    /**
     * Converts OCR results to an Excel spreadsheet using Apache POI.
     */
    fun ocrToExcel(ocrResult: com.plainlabs.qrpdftools.conversion.OcrEngine.OcrResult, outputPath: String): Boolean {
        // ... (Existing POI logic is fine)
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("OCR Result")
            
            val lines = ocrResult.text.split("\n")
            var rowNum = 0
            for (line in lines) {
                if (line.isNotBlank()) {
                    val row = sheet.createRow(rowNum++)
                    val cells = line.trim().split("\\s+".toRegex())
                    for ((colNum, cellValue) in cells.withIndex()) {
                        row.createCell(colNum).setCellValue(cellValue)
                    }
                }
            }
            
            FileOutputStream(outputPath).use { out ->
                workbook.write(out)
            }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
             false
        }
    }

    /**
     * Converts an Excel file to PDF using OpenPDF PdfPTable.
     */
    fun excelToPdf(inputPath: String, outputPath: String): Boolean {
        return try {
            FileInputStream(inputPath).use { fis ->
                val workbook = XSSFWorkbook(fis)
                val sheet = workbook.getSheetAt(0)
                
                val document = Document()
                PdfWriter.getInstance(document, FileOutputStream(outputPath))
                document.open()
                
                // Estimate columns from the first row or sheet max
                val firstRow = sheet.getRow(0)
                val numCols = firstRow?.lastCellNum?.toInt()?.coerceAtLeast(1) ?: 1
                val table = PdfPTable(numCols)
                table.widthPercentage = 100f
                
                for (row in sheet) {
                     for (i in 0 until numCols) {
                         val cell = row.getCell(i)
                         table.addCell(cell?.toString() ?: "")
                     }
                }
                
                document.add(table)
                document.close()
                workbook.close()
             }
             true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}