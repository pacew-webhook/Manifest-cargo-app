package com.example.cargostowing.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.cargostowing.data.CargoDao
import com.example.cargostowing.data.CargoItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CargoInput : Screen("cargo_input")
    object StowingChecklist : Screen("stowing_checklist/{manifestNo}") {
        fun createRoute(manifestNo: String) = "stowing_checklist/$manifestNo"
    }
}

class CargoViewModel(private val dao: CargoDao) : ViewModel() {
    fun getCargoItems(manifestNo: String): Flow<List<CargoItemEntity>> = dao.getCargoByManifest(manifestNo)
    fun insertCargo(item: CargoItemEntity) = viewModelScope.launch { dao.insertCargo(item) }
    fun updateStowStatus(id: Long, isStowed: Boolean) = viewModelScope.launch { dao.updateStowStatus(id, isStowed) }
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
            CargoInputScreen(onSave = { item ->
                viewModel.insertCargo(item)
                navController.popBackStack()
            })
        }

        composable(Screen.StowingChecklist.route) { backStackEntry ->
            val manifestNo = backStackEntry.arguments?.getString("manifestNo") ?: defaultManifestNo
            val items by viewModel.getCargoItems(manifestNo).collectAsState(initial = emptyList())
            StowingChecklistScreen(
                manifestNo = manifestNo,
                cargoItems = items,
                onToggleStow = { item, stowed -> viewModel.updateStowStatus(item.id, stowed) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoInputScreen(onSave: (CargoItemEntity) -> Unit) {
    var ptiNo by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pcsStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Input Cargo Baru") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = ptiNo, onValueChange = { ptiNo = it }, label = { Text("No. PTI") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Nama Customer") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi Barang") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pcsStr, onValueChange = { pcsStr = it }, label = { Text("Pcs / Cly") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = weightStr, onValueChange = { weightStr = it }, label = { Text("Berat Total (Kg)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val pcs = pcsStr.toIntOrNull() ?: 0
                    val weight = weightStr.toDoubleOrNull() ?: 0.0
                    if (ptiNo.isNotBlank()) {
                        onSave(
                            CargoItemEntity(
                                manifestOwnerNo = "MYI-KAL/100716/XII/2025",
                                ptiNo = ptiNo,
                                pcsCly = pcs,
                                weightPerPcs = null,
                                subTotalWeight = weight,
                                description = description,
                                customerName = customerName
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan Item Cargo") }
        }
    }
}
