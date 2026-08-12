package com.example.cargostowing.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cargostowing.data.CargoItemEntity
import com.example.cargostowing.util.CargoExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StowingChecklistScreen(
    manifestNo: String,
    cargoItems: List<CargoItemEntity>,
    onToggleStow: (CargoItemEntity, Boolean) -> Unit
) {
    val context = LocalContext.current
    val exporter = CargoExporter(context)

    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let { exporter.exportToExcelCustom(it, cargoItems, manifestNo = manifestNo) } }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { exporter.exportToPdf(it, cargoItems, manifestNo) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ceklis Stowing & Ekspor") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { excelLauncher.launch("Manifest_$manifestNo.xlsx") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(4.dp)); Text("Excel")
                }
                OutlinedButton(onClick = { pdfLauncher.launch("Stowing_$manifestNo.pdf") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Description, null); Spacer(Modifier.width(4.dp)); Text("PDF")
                }
            }

            LazyColumn {
                items(cargoItems) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.isStowed, onCheckedChange = { onToggleStow(item, it) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.ptiNo, style = MaterialTheme.typography.titleMedium)
                            Text("${item.customerName} - ${item.description}")
                        }
                        Text("${item.subTotalWeight} Kg")
                    }
                }
            }
        }
    }
}
