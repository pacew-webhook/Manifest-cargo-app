package com.example.cargostowing.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.cargostowing.data.CargoItemEntity
import org.apache.poi.ss.util.CellRangeAddress
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
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Manifest")

        sheet.createRow(5).createCell(0).setCellValue("MANIFEST CARGO")
        val r7 = sheet.createRow(6)
        r7.createCell(0).setCellValue(manifestNo)
        r7.createCell(7).setCellValue("STOWING CHEKLIST")

        val r8 = sheet.createRow(7)
        r8.createCell(1).setCellValue("DATE"); r8.createCell(2).setCellValue(": $dateStr")
        r8.createCell(5).setCellValue("A/C REG"); r8.createCell(6).setCellValue(": $acReg")
        r8.createCell(7).setCellValue("DATE"); r8.createCell(8).setCellValue(": $dateStr")

        val r9 = sheet.createRow(8)
        r9.createCell(1).setCellValue("FROM"); r9.createCell(2).setCellValue(": DJJ")
        r9.createCell(5).setCellValue("FLIGHT NO"); r9.createCell(6).setCellValue(": $flightNo")
        r9.createCell(7).setCellValue("FROM"); r9.createCell(8).setCellValue(": DJJ")

        val r12 = sheet.createRow(11)
        val headersLeft = arrayOf("No", "PTI", "Pcs/ Cly", "WEIGHT (Kg)", "", "DESCRIPTION", "COSTUMERS")
        headersLeft.forEachIndexed { i, t -> r12.createCell(i).setCellValue(t) }
        val headersRight = arrayOf("No", "NO PAG", "DESCRIPTION", "WEIGHT (Kg)")
        headersRight.forEachIndexed { i, t -> r12.createCell(i + 7).setCellValue(t) }

        sheet.addMergedRegion(CellRangeAddress(11, 11, 3, 4))

        var totalWeight = 0.0
        cargoItems.forEachIndexed { i, item ->
            val row = sheet.createRow(13 + i)
            row.createCell(0).setCellValue((i + 1).toDouble())
            row.createCell(1).setCellValue(item.ptiNo)
            row.createCell(2).setCellValue(item.pcsCly.toDouble())
            row.createCell(4).setCellValue(item.subTotalWeight)
            row.createCell(5).setCellValue(item.description)
            row.createCell(6).setCellValue(item.customerName)

            if (item.pagNo != null) {
                row.createCell(7).setCellValue((i + 1).toDouble())
                row.createCell(8).setCellValue(item.pagNo)
                row.createCell(9).setCellValue(item.description)
                row.createCell(10).setCellValue(item.subTotalWeight)
            }
            totalWeight += item.subTotalWeight
        }

        val totalRow = sheet.createRow(14 + cargoItems.size)
        totalRow.createCell(0).setCellValue("TOTAL WEIGHT")
        totalRow.createCell(4).setCellValue(totalWeight)

        context.contentResolver.openOutputStream(uri)?.use { workbook.write(it) }
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
            canvas.drawText("${it.ptiNo} | ${it.customerName} | ${it.description} | ${it.subTotalWeight} Kg | Status: ${if (it.isStowed) "STOWED" else "PENDING"}", 40f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }
}
