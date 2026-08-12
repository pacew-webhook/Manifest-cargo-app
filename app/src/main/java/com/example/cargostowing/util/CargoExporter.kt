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

        // Header Info
        val row7 = sheet.getRow(6) ?: sheet.createRow(6)
        (row7.getCell(0) ?: row7.createCell(0)).setCellValue(manifestNo)

        val row8 = sheet.getRow(7) ?: sheet.createRow(7)
        (row8.getCell(2) ?: row8.createCell(2)).setCellValue(": $dateStr")
        (row8.getCell(6) ?: row8.createCell(6)).setCellValue(": $acReg")

        val row9 = sheet.getRow(8) ?: sheet.createRow(8)
        (row9.getCell(6) ?: row9.createCell(6)).setCellValue(": $flightNo")

        // 1. MANIFEST CARGO (SISI KIRI) - Digabung per PTI & Customer
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

        groupedManifestItems.forEachIndexed { i, item ->
            val rowIndex = 13 + i
            val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

            (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble())
            (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)
            (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble())
            (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight)
            (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)
            (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)
        }

        // 2. STOWING CHECKLIST (SISI KANAN) - DIGABUNGKAN PER NO. PAG SAMA
        val groupedStowingItems = cargoItems.groupBy {
            it.pagNo?.trim()?.uppercase() ?: "TANPA PAG"
        }.map { (pag, items) ->
            val first = items.first()
            // Menggabungkan seluruh deskripsi unik yang masuk dalam PAG yang sama
            val combinedDesc = items.map { it.description.trim() }.distinct().joinToString(", ")
            first.copy(
                pagNo = if (pag == "TANPA PAG") null else pag,
                description = combinedDesc,
                pcsCly = items.sumOf { it.pcsCly },
                subTotalWeight = items.sumOf { it.subTotalWeight }
            )
        }

        groupedStowingItems.forEachIndexed { i, item ->
            val rowIndex = 13 + i
            val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

            // Kolom I : NO PAG
            (row.getCell(8) ?: row.createCell(8)).setCellValue(item.pagNo ?: "")

            // Kolom J : DESCRIPTION
            (row.getCell(9) ?: row.createCell(9)).setCellValue(item.description)

            // Kolom K : WEIGHT Net
            (row.getCell(10) ?: row.createCell(10)).setCellValue(item.subTotalWeight)

            // Kolom L : WEIGHT Gross
            (row.getCell(11) ?: row.createCell(11)).setCellValue(item.subTotalWeight)

            // Kolom M : COSTUMERS
            (row.getCell(12) ?: row.createCell(12)).setCellValue(item.customerName)
        }

        // 3. SHEET "DATA CUSTOMER" (DETAIL ASLI TANPA DIGABUNG)
        val customerSheet = try {
            workbook.getSheet("DATA CUSTOMER") ?: if (workbook.numberOfSheets > 1) workbook.getSheetAt(1) else null
        } catch (e: Exception) {
            null
        }

        customerSheet?.let { custSheet ->
            val custRow7 = custSheet.getRow(6) ?: custSheet.createRow(6)
            (custRow7.getCell(0) ?: custRow7.createCell(0)).setCellValue(manifestNo)

            cargoItems.forEachIndexed { i, item ->
                val rowIndex = 13 + i
                val row = custSheet.getRow(rowIndex) ?: custSheet.createRow(rowIndex)

                (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble())
                (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)
                (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble())
                (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight)
                (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)
                (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)
            }
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
        
        // Grouping berdasarkan PAG untuk tampilan PDF
        val groupedPdfItems = items.groupBy { it.pagNo?.trim()?.uppercase() ?: "TANPA PAG" }
            .map { (pag, list) ->
                val totalWeight = list.sumOf { it.subTotalWeight }
                val combinedDesc = list.map { it.description }.distinct().joinToString(", ")
                "$pag | $combinedDesc | Total Berat: $totalWeight Kg"
            }

        groupedPdfItems.forEach { line ->
            canvas.drawText(line, 40f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }
}
