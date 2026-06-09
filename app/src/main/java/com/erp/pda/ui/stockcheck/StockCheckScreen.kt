package com.erp.pda.ui.stockcheck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import com.erp.pda.ui.components.IosTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCheckScreen(
    scannerManager: ScannerManager,
    onBack: () -> Unit = {},
    viewModel: StockCheckViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            ScanFeedback.success()
            viewModel.searchProducts(result.code)
        }
    }

    Scaffold(
        topBar = {
            IosTopBar(
                title = "快速查庫存",
                onBack = { if (state.selectedProduct != null) viewModel.clearProduct() else onBack() }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.selectedProduct == null) {
                // Search + results
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::searchProducts,
                    label = { Text("掃描條碼 或 輸入關鍵字") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (state.isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else LazyColumn {
                    items(state.products) { product ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectProduct(product) }) {
                            Row(Modifier.padding(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.skuCode, fontWeight = FontWeight.Bold)
                                    Text(product.nameZh, style = MaterialTheme.typography.bodySmall)
                                }
                                if (product.isSerialTracked) Text("S/N", style = MaterialTheme.typography.labelSmall, color = Primary)
                            }
                        }
                    }
                }
            } else {
                // Stock detail
                val prod = state.selectedProduct!!
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TileTeal.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prod.skuCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (prod.isSerialTracked) {
                                AssistChip(onClick = {}, label = { Text("S/N") },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Primary.copy(alpha = 0.15f)))
                            }
                        }
                        Text(prod.nameZh, style = MaterialTheme.typography.bodyMedium)
                        if (prod.nameEn.isNotBlank() && prod.nameEn != prod.nameZh) {
                            Text(prod.nameEn, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                state.stocks.forEach { stock ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stock.warehouseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StockBox("實際庫存", stock.physicalStock, Success)
                                StockBox("已鎖定", stock.allocatedStock, Warning)
                                StockBox("已承諾", stock.committedStock, Primary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("可售: ${stock.available}", fontWeight = FontWeight.Bold, color = if (stock.available > 0) Success else Error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockBox(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
