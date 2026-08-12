package com.example.cargostowing.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    flightNo: String = "3Y704",
    acReg: String = "PK-MYE",
    route: String = "DJJ ➔ WMX",
    dateStr: String = "11/12/2025",
    totalItems: Int = 0,
    totalWeightKg: Double = 0.0,
    stowedCount: Int = 0,
    onNavigateToInput: () -> Unit,
    onNavigateToChecklist: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val progressFraction = if (totalItems > 0) stowedCount.toFloat() / totalItems else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CargoStow Ops", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Sistem Manajemen Manifest & Stowing", fontSize = 12.sp)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FLIGHT AKTIF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(dateStr, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(flightNo, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("A/C: $acReg", fontSize = 14.sp)
                            }
                            Text(route, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Kargo: $totalItems Item / $totalWeightKg Kg", fontSize = 13.sp)
                            Text("Stowed: $stowedCount", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                    }
                }
            }

            item { Text("Menu Utama", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuActionCard("Input Cargo", "Tambah item baru", Icons.Default.PostAdd, Modifier.weight(1f), onNavigateToInput)
                    MenuActionCard("Ceklis Stowing", "Verifikasi muatan", Icons.Default.Checklist, Modifier.weight(1f), onNavigateToChecklist)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuActionCard("Ekspor Laporan", "PDF & Excel Custom", Icons.Default.FileUpload, Modifier.weight(1f), onNavigateToExport)
                    MenuActionCard("Arsip Flight", "Riwayat penerbangan", Icons.Default.History, Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
fun MenuActionCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp)
            }
        }
    }
}
