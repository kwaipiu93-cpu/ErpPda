package com.erp.pda.ui.stocktake

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.erp.pda.ui.theme.Primary
import com.erp.pda.ui.theme.Success
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocktakeScreen(
    scannerManager: ScannerManager,
    viewModel: StocktakeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWarehouses()
        scannerManager.scanResults.collect { result ->
            viewModel.scanBarcode(result.code)
            ScanFeedback.success()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("庫存盤點") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            state.feedback?.let {
                Surface(Modifier.fillMaxWidth(), color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                    Text(it, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            if (state.activeTask == null) {
                // Setup: select warehouse + create task
                Text("建立盤點任務", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("選擇倉庫:", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                state.warehouses.forEach { wh ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.selectWarehouse(wh.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.selectedWarehouseId == wh.id) Primary.copy(alpha = 0.2f) else Color.Transparent
                        )
                    ) {
                        Text("${wh.nameZh} (${wh.nameEn})", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::createTask,
                    enabled = state.selectedWarehouseId != null && !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("開始盤點")
                }
            } else {
                // Active task — scanning mode
                val task = state.activeTask!!
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(task.stkNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("狀態: ${task.fsmStatus}")
                        Text("已掃描: ${state.scannedCount} 件", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = Primary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                state.lastScanned?.let {
                    Card(Modifier.fillMaxWidth()) {
                        Text("最近掃描: $it", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = viewModel::completeTask,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success)
                ) {
                    Text("完成盤點")
                }
            }
        }
    }
}
