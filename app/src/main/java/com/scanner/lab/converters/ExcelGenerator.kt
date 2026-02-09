package com.scanner.lab.converters

import android.content.Context
import android.net.Uri
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

/**
 * Excel Generator using Apache POI
 * Converts Text Data to .xlsx
 */
object ExcelGenerator {

    /**
     * Generate Excel from list of text blocks
     */
    fun generateExcel(
        context: Context,
        data: List<String>,
        outputStream: OutputStream
    ): Result<Boolean> = runCatching {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Scanned Data")
        
        // Header
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Scanned Content")
        
        // Data
        data.forEachIndexed { index, text ->
            val row = sheet.createRow(index + 1)
            // Split by newlines to create multiple rows?
            // Or just put block in one cell?
            // Let's put strictly in one cell for now, simplifying.
            row.createCell(0).setCellValue(text)
        }
        
        workbook.write(outputStream)
        workbook.close()
        true
    }
}
