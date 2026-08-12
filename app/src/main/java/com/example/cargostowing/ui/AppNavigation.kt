package com.example.cargostowing.ui

import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.cargostowing.data.CargoDao
import com.example.cargostowing.data.CargoItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

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

class CargoViewModel(private val dao: CargoDao) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun getCargoItems(manifestNo: String): Flow<List<CargoItemEntity>> = dao.getCargoByManifest(manifestNo)

    fun insertCargo(item: CargoItemEntity, onSuccess: () -> Unit) = viewModelScope.launch {
        val cleanDesc = item.description.trim()
        val cleanCust = item.customerName.trim()
        val cleanPag = item.pagNo?.trim()

        val duplicateItem = dao.findCargoByDescAndCustomer(
            manifestNo = item.manifestOwnerNo,
            description = cleanDesc,
            customerName = cleanCust,
            pagNo = cleanPag
        )

        if (duplicateItem != null && !duplicateItem.ptiNo.equals(item.ptiNo.trim(), ignoreCase = true)) {
            _uiEvent.emit("Gagal Simpan: Barang ($cleanDesc) untuk customer ($cleanCust) sudah terdaftar dengan No. PTI (${duplicateItem.ptiNo})!")
        } else {
            dao.insertCargo(item)
            onSuccess()
        }
    }

    fun updateCargoItem(item: CargoItemEntity) = viewModelScope.launch {
        dao.updateCargo(item)
    }

    fun updateStowStatus(id: Long, isStowed: Boolean) = viewModelScope.launch {
        dao.updateStowStatus(id, isStowed)
    }

    fun deleteCargoItem(item: CargoItemEntity) = viewModelScope.launch {
        dao.deleteCargo(item)
    }
}

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
                onNavigateToChecklist = { navController.navigate(Screen.StowingChecklist.createRoute(defaultManifestNo)) },
                onNavigateToExport = { navController.navigate(Screen.StowingChecklist.createRoute(defaultManifestNo)) }
            )
        }

        composable(Screen.CargoInput.route) {
            val items by viewModel.getCargoItems(defaultManifestNo).collectAsState(initial = emptyList())
            CargoInputScreen(
                existingItems = items,
                viewModel = viewModel,
                onSaveSuccess = { }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoInputScreen(
    existingItems: List<CargoItemEntity> = emptyList(),
    viewModel: CargoViewModel,
    onSaveSuccess: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Long
            )
        }
    }

    val autoPtiNumber = remember(existingItems) {
        if (existingItems.isEmpty()) {
            "001"
        } else {
            val maxNum = existingItems.mapNotNull { item ->
                item.ptiNo.replace("[^0-9]".toRegex(), "").toIntOrNull()
            }.maxOrNull() ?: existingItems.size
            
            String.format("%03d", maxNum + 1)
        }
    }

    var ptiNo by remember(autoPtiNumber) { mutableStateOf(autoPtiNumber) }
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

    val clearFields = {
        pagNo = ""
        description = ""
        pcsStr = ""
        weightStr = ""
    }

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
            val newItem = CargoItemEntity(
                manifestOwnerNo = "MYI-KAL/100716/XII/2025",
                ptiNo = finalPtiNo,
                pagNo = finalPagNo,
                pcsCly = pcs,
                weightPerPcs = null,
                subTotalWeight = weight,
                description = description.trim().uppercase(),
                customerName = customerName.trim().uppercase()
            )

            viewModel.insertCargo(newItem, onSuccess = {
                clearFields()
                onSaveSuccess()
            })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Input Cargo Baru") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = ptiNo,
                    onValueChange = { input ->
                        ptiNo = input.uppercase()

                        val cleanInputPti = if (input.uppercase().startsWith("KAL")) input.uppercase() else "KAL${input.uppercase()}"
                        val match = existingItems.find { 
                            it.ptiNo.equals(cleanInputPti.trim(), ignoreCase = true) ||
                            it.ptiNo.removePrefix("KAL").equals(input.trim(), ignoreCase = true)
                        }
                        if (match != null && customerName.isBlank()) {
                            customerName = match.customerName
                        }
                    },
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
            }

            item {
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
            }

            item {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { input ->
                        val upperInput = input.uppercase()
                        customerName = upperInput

                        val existingCustomer = existingItems.find { 
                            it.customerName.trim().equals(upperInput.trim(), ignoreCase = true) 
                        }

                        if (existingCustomer != null) {
                            ptiNo = existingCustomer.ptiNo.removePrefix("KAL")
                        } else {
                            ptiNo = autoPtiNumber
                        }
                    },
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
            }

            item {
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
            }

            item {
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
            }

            item {
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
            }

            item {
                Button(
                    onClick = submitAction,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simpan Item Cargo") }
            }

            if (existingItems.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Daftar Item Cargo / Stowing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                items(existingItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = item.isStowed,
                                    onCheckedChange = { isChecked ->
                                        viewModel.updateStowStatus(item.id, isChecked)
                                    }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        "${item.ptiNo} | ${item.pagNo ?: "TANPA PAG"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "${item.customerName} - ${item.description}",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Jumlah: ${item.pcsCly} Pcs",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${item.subTotalWeight} Kg",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(onClick = { viewModel.deleteCargoItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
