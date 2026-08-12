package com.example.cargostowing.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.cargostowing.data.CargoItemEntity
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class CargoExporter(private val context: Context) {

    fun exportToExcelCustom(
        uri: Uri,
        cargoItems: List<CargoItemEntity>,
        manifestNo: String = "MYI-KAL/100716/XII/2025",
        flightNo: String = "3Y704",
        acReg: String = "PK-MYE",
        dateStr: String = "11/12/2025"
    ) {
        val workbook = try {
            val inputStream = context.assets.open("template_manifest.xlsx")
            XSSFWorkbook(inputStream)
        } catch (e: Exception) {
            XSSFWorkbook()
        }

        // =================================================================
        // 1. SHEET "Manifest" (DATA DIGABUNG BILA PTI & CUSTOMER SAMA)
        // =================================================================
        val manifestSheet = workbook.getSheet("Manifest") ?: workbook.getSheetAt(0)

        // Header Manifest & Flight Info
        val row7 = manifestSheet.getRow(6) ?: manifestSheet.createRow(6)
        (row7.getCell(0) ?: row7.createCell(0)).setCellValue(manifestNo)

        val row8 = manifestSheet.getRow(7) ?: manifestSheet.createRow(7)
        (row8.getCell(2) ?: row8.createCell(2)).setCellValue(": $dateStr")
        (row8.getCell(6) ?: row8.createCell(6)).setCellValue(": $acReg")

        val row9 = manifestSheet.getRow(8) ?: manifestSheet.createRow(8)
        (row9.getCell(6) ?: row9.createCell(6)).setCellValue(": $flightNo")

        // Grouping berdasarkan PTI, Customer, dan Description
        val groupedManifestItems = cargoItems.groupBy {
            Triple(
                it.ptiNo.trim().uppercase(),
                it.customerName.trim().uppercase(),
                it.description.trim().uppercase()
            )
        }.map { (_, items) ->
            val first = items.first()
            first.copy(
                pcsCly = items.sumOf { it.pcsCly },
                subTotalWeight = items.sumOf { it.subTotalWeight }
            )
        }

        // Tulis data gabungan ke Sheet Manifest
        groupedManifestItems.forEachIndexed { i, item ->
            val rowIndex = 13 + i
            val row = manifestSheet.getRow(rowIndex) ?: manifestSheet.createRow(rowIndex)

            (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble()) // Kolom A: No
            (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)         // Kolom B: PTI
            (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble()) // Kolom C: Pcs/Cly
            (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight) // Kolom E: Sub Total Weight
            (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)   // Kolom F: DESCRIPTION
            (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)  // Kolom G: COSTUMERS
        }

        // =================================================================
        // 2. SHEET "DATA CUSTOMER" (DATA DIPISAH PER INPUTAN ASLI)
        // =================================================================
        val customerSheet = if (workbook.numberOfSheets > 1) {
            workbook.getSheet("DATA CUSTOMER") ?: workbook.getSheetAt(1)
        } else {
            workbook.getSheet("DATA CUSTOMER")
        }

        customerSheet?.let { sheet ->
            // Header Info untuk Sheet Data Customer (jika ada struktur header yang sama)
            val custRow7 = sheet.getRow(6) ?: sheet.createRow(6)
            (custRow7.getCell(0) ?: custRow7.createCell(0)).setCellValue(manifestNo)

            val custRow8 = sheet.getRow(7) ?: sheet.createRow(7)
            (custRow8.getCell(2) ?: custRow8.createCell(2)).setCellValue(": $dateStr")
            (custRow8.getCell(6) ?: custRow8.createCell(6)).setCellValue(": $acReg")

            val custRow9 = sheet.getRow(8) ?: sheet.createRow(8)
            (custRow9.getCell(6) ?: custRow9.createCell(6)).setCellValue(": $flightNo")

            // Tulis data asli tanpa digabung
            cargoItems.forEachIndexed { i, item ->
                val rowIndex = 13 + i
                val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

                (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble()) // Kolom A: No
                (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)         // Kolom B: PTI
                (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble()) // Kolom C: Pcs/Cly
                (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight) // Kolom E: Sub Total Weight
                (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)   // Kolom F: DESCRIPTION
                (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)  // Kolom G: COSTUMERS
            }
        }

        // Simpan File
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()
    }

    fun exportToPdf(uri: Uri, items: List<CargoItemEntity>, manifestNo: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { textSize = 10f }

        canvas.drawText("MANIFEST & STOWING CHECKLIST - $manifestNo", 40f, 50f, paint)
        var y = 90f
        items.forEach {
            canvas.drawText("${it.ptiNo} | PAG: ${it.pagNo ?: "-"} | ${it.customerName} | ${it.description} | ${it.subTotalWeight} Kg | Status: ${if (it.isStowed) "STOWED" else "PENDING"}", 40f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }
}
