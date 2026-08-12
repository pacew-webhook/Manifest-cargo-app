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

        val sheet = workbook.getSheet("Manifest") ?: workbook.getSheetAt(0)

        // Header Flight & Manifest
        val row6 = sheet.getRow(6) ?: sheet.createRow(6)
        (row6.getCell(0) ?: row6.createCell(0)).setCellValue(manifestNo)

        val row7 = sheet.getRow(7) ?: sheet.createRow(7)
        (row7.getCell(2) ?: row7.createCell(2)).setCellValue(": $dateStr")
        (row7.getCell(6) ?: row7.createCell(6)).setCellValue(": $acReg")

        val row8 = sheet.getRow(8) ?: sheet.createRow(8)
        (row8.getCell(6) ?: row8.createCell(6)).setCellValue(": $flightNo")

        // Grouping berdasarkan Description, Customer, dan No PAG
        val groupedItems = cargoItems.groupBy {
            Triple(
                it.description.uppercase().trim(),
                it.customerName.uppercase().trim(),
                it.pagNo?.uppercase()?.trim() ?: ""
            )
        }.map { (_, items) ->
            items.first().copy(
                pcsCly = items.sumOf { it.pcsCly },
                subTotalWeight = items.sumOf { it.subTotalWeight }
            )
        }

        // Tulis Data Hasil Penggabungan ke Excel
        groupedItems.forEachIndexed { i, item ->
            val rowIndex = 13 + i
            val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

            // Sisi Manifest (Kiri)
            (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble())
            (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)
            (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble())
            (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight)
            (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)
            (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)

            // Sisi Stowing Checklist (Kanan) - Dikembalikan ke indeks awal
            (row.getCell(7) ?: row.createCell(7)).setCellValue((i + 1).toDouble())
            if (!item.pagNo.isNullOrEmpty()) {
                (row.getCell(8) ?: row.createCell(8)).setCellValue(item.pagNo)
            }
            (row.getCell(9) ?: row.createCell(9)).setCellValue(item.description)
            (row.getCell(10) ?: row.createCell(10)).setCellValue(item.subTotalWeight)
            (row.getCell(12) ?: row.createCell(12)).setCellValue(item.customerName)
        }

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
