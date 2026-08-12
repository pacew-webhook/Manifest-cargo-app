package com.example.cargostowing.ui

import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.cargostowing.data.CargoDao
import com.example.cargostowing.data.CargoItemEntity
import com.example.cargostowing.util.CargoExporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// =========================================================================
// 1. RUTE NAVIGASI
// =========================================================================
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CargoInput : Screen("cargo_input")
    object StowingChecklist : Screen("stowing_checklist?manifestNo={manifestNo}") {
        fun createRoute(manifestNo: String): String {
            val encoded = URLEncoder.encode(manifestNo, StandardCharsets.UTF_8.toString())
            return "stowing_checklist?manifestNo=$encoded"
        }
    }
}

// =========================================================================
// 2. VIEWMODEL (LOGIKA DATA & DATABASE)
// =========================================================================
class CargoViewModel(private val dao: CargoDao) : ViewModel() {
    
    fun getCargoItems(manifestNo: String): Flow<List<CargoItemEntity>> = dao.getCargoByManifest(manifestNo)

    fun insertCargo(item: CargoItemEntity) = viewModelScope.launch {
        val existing = dao.findCargoByDescAndCustomer(
            manifestNo = item.manifestOwnerNo,
            description = item.description.trim(),
            customerName = item.customerName.trim(),
            pagNo = item.pagNo?.trim()
        )
        if (existing != null) {
            val updated = existing.copy(
                pcsCly = existing.pcsCly + item.pcsCly,
                subTotalWeight = existing.subTotalWeight + item.subTotalWeight
            )
            dao.updateCargo(updated)
        } else {
            dao.insertCargo(item)
        }
    }

    fun updateCargoItem(item: CargoItemEntity) = viewModelScope.launch {
        dao.updateCargo(item)
    }

    fun updateStowStatus(id: Long, isStowed: Boolean) = viewModelScope.launch {
        dao.updateStowStatus(id, isStowed)
    }
}

// =========================================================================
// 3. NAV HOST (PENGATUR NAVIGASI APLIKASI)
// =========================================================================
@Composable
fun AppNavHost(navController: NavHostController, viewModel: CargoViewModel) {
    val defaultManifestNo = "MYI-KAL/100716/XII/2025"

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val items by viewModel.getCargoItems(defaultManifestNo).collectAsState(initial = emptyList())
            HomeScreen(
                totalItems = items.size,
                totalWeightKg = items.sumOf { it.subTotalWeight },
                stowedCount = items.count { it.isStowed },
                onNavigateToInput = { navController.navigate(Screen.CargoInput.route) },
                onNavigateToChecklist = { navController.navigate(Screen.StowingChecklist.createRoute(defaultManifestNo)) }
            )
        }

        composable(Screen.CargoInput.route) {
            CargoInputScreen(onSave = { item ->
                viewModel.insertCargo(item)
                navController.popBackStack()
            })
        }

        composable(
            route = Screen.StowingChecklist.route,
            arguments = listOf(
                navArgument("manifestNo") {
                    type = NavType.StringType
                    defaultValue = defaultManifestNo
                }
            )
        ) { backStackEntry ->
            val rawManifestNo = backStackEntry.arguments?.getString("manifestNo") ?: defaultManifestNo
            val manifestNo = try {
                URLDecoder.decode(rawManifestNo, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                rawManifestNo
            }
            val items by viewModel.getCargoItems(manifestNo).collectAsState(initial = emptyList())
            
            StowingChecklistScreen(
                manifestNo = manifestNo,
                cargoItems = items,
                onToggleStow = { item, stowed -> viewModel.updateStowStatus(item.id, stowed) },
                onUpdateCargo = { updatedItem -> viewModel.updateCargoItem(updatedItem) }
            )
        }
    }
}

// =========================================================================
// 4. LAYAR UTAMA (HOME)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    totalItems: Int,
    totalWeightKg: Double,
    stowedCount: Int,
    onNavigateToInput: () -> Unit,
    onNavigateToChecklist: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Cargo Stowing") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ringkasan Cargo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Total Item: $totalItems")
                    Text("Total Berat: $totalWeightKg Kg")
                    Text("Sudah Stowed: $stowedCount / $totalItems")
                }
            }
            Button(onClick = onNavigateToInput, modifier = Modifier.fillMaxWidth()) {
                Text("Input Cargo Baru")
            }
            OutlinedButton(onClick = onNavigateToChecklist, modifier = Modifier.fillMaxWidth()) {
                Text("Ceklis Stowing & Ekspor")
            }
        }
    }
}

// =========================================================================
// 5. LAYAR INPUT CARGO BARU
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoInputScreen(onSave: (CargoItemEntity) -> Unit) {
    var ptiNo by remember { mutableStateOf("") }
    var pagNo by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pcsStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }

    val focusPag = remember { FocusRequester() }
    val focusCustomer = remember { FocusRequester() }
    val focusDesc = remember { FocusRequester() }
    val focusPcs = remember { FocusRequester() }
    val focusWeight = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val submitAction = {
        val pcs = pcsStr.toIntOrNull() ?: 0
        val weight = weightStr.toDoubleOrNull() ?: 0.0

        val cleanPti = ptiNo.trim().uppercase()
        val finalPtiNo = when {
            cleanPti.isBlank() -> ""
            cleanPti.startsWith("KAL") -> cleanPti
            else -> "KAL$cleanPti"
        }

        val cleanPag = pagNo.trim().uppercase()
        val finalPagNo = when {
            cleanPag.isBlank() -> null
            cleanPag.startsWith("PAG") -> cleanPag
            else -> "PAG $cleanPag"
        }

        if (finalPtiNo.isNotBlank()) {
            onSave(
                CargoItemEntity(
                    manifestOwnerNo = "MYI-KAL/100716/XII/2025",
                    ptiNo = finalPtiNo,
                    pagNo = finalPagNo,
                    pcsCly = pcs,
                    weightPerPcs = null,
                    subTotalWeight = weight,
                    description = description.trim().uppercase(),
                    customerName = customerName.trim().uppercase()
                )
            )
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Input Cargo Baru") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ptiNo,
                onValueChange = { ptiNo = it.uppercase() },
                label = { Text("No. PTI") },
                prefix = { Text("KAL") },
                placeholder = { Text("001") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusPag.requestFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pagNo,
                onValueChange = { pagNo = it.uppercase() },
                label = { Text("No. PAG (Opsional)") },
                prefix = { Text("PAG ") },
                placeholder = { Text("002 MYI") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusCustomer.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusPag)
            )

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it.uppercase() },
                label = { Text("Nama Customer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusDesc.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusCustomer)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.uppercase() },
                label = { Text("Deskripsi Barang") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusPcs.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusDesc)
            )

            OutlinedTextField(
                value = pcsStr,
                onValueChange = { pcsStr = it },
                label = { Text("Pcs / Cly") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusWeight.requestFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusPcs)
            )

            OutlinedTextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                label = { Text("Berat Total (Kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    submitAction()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusWeight)
            )

            Button(
                onClick = submitAction,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan Item Cargo") }
        }
    }
}

// =========================================================================
// 6. LAYAR CEKLIS STOWING & EDIT ITEM
// =========================================================================
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
                            Text(
                                text = "${item.ptiNo}${if (!item.pagNo.isNullOrBlank()) " | ${item.pagNo}" else ""}",
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

// =========================================================================
// 7. DIALOG EDIT CARGO
// =========================================================================
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
    
