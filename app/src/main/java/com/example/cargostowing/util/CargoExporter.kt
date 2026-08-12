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
        val sheet = workbook.getSheet("Manifest") ?: workbook.getSheetAt(0)

        // Header Manifest & Flight Info
        val row7 = sheet.getRow(6) ?: sheet.createRow(6)
        (row7.getCell(0) ?: row7.createCell(0)).setCellValue(manifestNo)

        val row8 = sheet.getRow(7) ?: sheet.createRow(7)
        (row8.getCell(2) ?: row8.createCell(2)).setCellValue(": $dateStr")
        (row8.getCell(6) ?: row8.createCell(6)).setCellValue(": $acReg")

        val row9 = sheet.getRow(8) ?: sheet.createRow(8)
        (row9.getCell(6) ?: row9.createCell(6)).setCellValue(": $flightNo")

        // -----------------------------------------------------------------
        // 1. MANIFEST CARGO (SISI KIRI) - Digabung per PTI & Customer
        // -----------------------------------------------------------------
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

            (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble()) // Kolom A: No
            (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)         // Kolom B: PTI
            (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble()) // Kolom C: Pcs/Cly
            (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight) // Kolom E: Weight Sub Total
            (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)   // Kolom F: DESCRIPTION
            (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)  // Kolom G: COSTUMERS
        }

        // -----------------------------------------------------------------
        // 2. STOWING CHECKLIST (SISI KANAN) - Memisah Baris Jika Nomor PAG Beda
        // -----------------------------------------------------------------
        val groupedStowingItems = cargoItems.groupBy {
            Triple(
                it.pagNo?.trim()?.uppercase() ?: "",
                it.description.trim().uppercase(),
                it.customerName.trim().uppercase()
            )
        }.map { (_, items) ->
            val first = items.first()
            first.copy(
                pcsCly = items.sumOf { it.pcsCly },
                subTotalWeight = items.sumOf { it.subTotalWeight }
            )
        }

        groupedStowingItems.forEachIndexed { i, item ->
            val rowIndex = 13 + i
            val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

            // Kolom I (Index 8) : NO PAG
            (row.getCell(8) ?: row.createCell(8)).setCellValue(item.pagNo ?: "")

            // Kolom J (Index 9) : DESCRIPTION
            (row.getCell(9) ?: row.createCell(9)).setCellValue(item.description)

            // Kolom K (Index 10): WEIGHT Net
            (row.getCell(10) ?: row.createCell(10)).setCellValue(item.subTotalWeight)

            // Kolom L (Index 11): WEIGHT Gross
            (row.getCell(11) ?: row.createCell(11)).setCellValue(item.subTotalWeight)

            // Kolom M (Index 12): COSTUMERS
            (row.getCell(12) ?: row.createCell(12)).setCellValue(item.customerName)
        }

        // =================================================================
        // 3. SHEET "DATA CUSTOMER" (DATA DIPISAH PER INPUTAN ASLI)
        // =================================================================
        val customerSheet = try {
            workbook.getSheet("DATA CUSTOMER") ?: if (workbook.numberOfSheets > 1) workbook.getSheetAt(1) else null
        } catch (e: Exception) {
            null
        }

        customerSheet?.let { custSheet ->
            // Header Info
            val custRow7 = custSheet.getRow(6) ?: custSheet.createRow(6)
            (custRow7.getCell(0) ?: custRow7.createCell(0)).setCellValue(manifestNo)

            val custRow8 = custSheet.getRow(7) ?: custSheet.createRow(7)
            (custRow8.getCell(2) ?: custRow8.createCell(2)).setCellValue(": $dateStr")
            (custRow8.getCell(6) ?: custRow8.createCell(6)).setCellValue(": $acReg")

            val custRow9 = custSheet.getRow(8) ?: custSheet.createRow(8)
            (custRow9.getCell(6) ?: custRow9.createCell(6)).setCellValue(": $flightNo")

            // Detail Per Item ASLI (Tidak Digabung)
            cargoItems.forEachIndexed { i, item ->
                val rowIndex = 13 + i
                val row = custSheet.getRow(rowIndex) ?: custSheet.createRow(rowIndex)

                (row.getCell(0) ?: row.createCell(0)).setCellValue((i + 1).toDouble()) // Kolom A: No
                (row.getCell(1) ?: row.createCell(1)).setCellValue(item.ptiNo)         // Kolom B: PTI
                (row.getCell(2) ?: row.createCell(2)).setCellValue(item.pcsCly.toDouble()) // Kolom C: Pcs/Cly
                (row.getCell(4) ?: row.createCell(4)).setCellValue(item.subTotalWeight) // Kolom E: Weight Sub Total
                (row.getCell(5) ?: row.createCell(5)).setCellValue(item.description)   // Kolom F: DESCRIPTION
                (row.getCell(6) ?: row.createCell(6)).setCellValue(item.customerName)  // Kolom G: COSTUMERS
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
        items.forEach {
            canvas.drawText("${it.ptiNo} | PAG: ${it.pagNo ?: "-"} | ${it.customerName} | ${it.description} | ${it.subTotalWeight} Kg | Status: ${if (it.isStowed) "STOWED" else "PENDING"}", 40f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }
}
