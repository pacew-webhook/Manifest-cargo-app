package com.example.cargostowing.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cargostowing.data.CargoItemEntity
import com.example.cargostowing.util.CargoExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StowingChecklistScreen(
    manifestNo: String,
    cargoItems: List<CargoItemEntity>,
    onToggleStow: (CargoItemEntity, Boolean) -> Unit,
    onUpdateCargo: (CargoItemEntity) -> Unit
) {
    val context = LocalContext.current
    val exporter = CargoExporter(context)

    // State untuk menyimpan item yang sedang diedit
    var itemToEdit by remember { mutableStateOf<CargoItemEntity?>(null) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { excelLauncher.launch("Manifest_$manifestNo.xlsx") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Excel")
                }
                OutlinedButton(
                    onClick = { pdfLauncher.launch("Stowing_$manifestNo.pdf") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Description, null)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
            }

            LazyColumn {
                items(cargoItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isStowed,
                            onCheckedChange = { onToggleStow(item, it) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            // Menampilkan PTI dan PAG
                            Text(
                                text = "${item.ptiNo}${if (!item.pagNo.isNull_Or_Empty()) " | ${item.pagNo}" else ""}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${item.customerName} - ${item.description}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Jumlah: ${item.pcsCly} Pcs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "${item.subTotalWeight} Kg",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { itemToEdit = item }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Item"
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    // Dialog Edit Item
    itemToEdit?.let { item ->
        EditCargoDialog(
            item = item,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                onUpdateCargo(updatedItem)
                itemToEdit = null
            }
        )
    }
}

// Helper Extension Function
private fun String?.isNull_Or_Empty(): Boolean = this.isNullOrBlank()

@Composable
fun EditCargoDialog(
    item: CargoItemEntity,
    onDismiss: () -> Unit,
    onSave: (CargoItemEntity) -> Unit
) {
    var ptiNo by remember { mutableStateOf(item.ptiNo) }
    var pagNo by remember { mutableStateOf(item.pagNo ?: "") }
    var customerName by remember { mutableStateOf(item.customerName) }
    var description by remember { mutableStateOf(item.description) }
    var pcsStr by remember { mutableStateOf(item.pcsCly.toString()) }
    var weightStr by remember { mutableStateOf(item.subTotalWeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Cargo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = ptiNo,
                    onValueChange = { ptiNo = it.uppercase() },
                    label = { Text("No. PTI") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedTextField(
                    value = pagNo,
                    onValueChange = { pagNo = it.uppercase() },
                    label = { Text("No. PAG") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it.uppercase() },
                    label = { Text("Customer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.uppercase() },
                    label = { Text("Deskripsi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                OutlinedTextField(
                    value = pcsStr,
                    onValueChange = { pcsStr = it },
                    label = { Text("Pcs / Cly") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Berat Total (Kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updated = item.copy(
                    ptiNo = ptiNo.trim().uppercase(),
                    pagNo = pagNo.trim().uppercase().ifBlank { null },
                    customerName = customerName.trim().uppercase(),
                    description = description.trim().uppercase(),
                    pcsCly = pcsStr.toIntOrNull() ?: item.pcsCly,
                    subTotalWeight = weightStr.toDoubleOrNull() ?: item.subTotalWeight
                )
                onSave(updated)
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
