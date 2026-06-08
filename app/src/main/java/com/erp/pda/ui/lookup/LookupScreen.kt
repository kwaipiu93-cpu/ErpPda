package com.erp.pda.ui.lookup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupScreen(
    scannerManager: ScannerManager,
    viewModel: LookupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            ScanFeedback.success()
            viewModel.setSerialAndLookup(result.code)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("S/N 查詢") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Search input
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.serialNumber,
                    onValueChange = viewModel::onSerialChange,
                    label = { Text("輸入或掃描 S/N") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = viewModel::lookup, enabled = !state.isLoading) {
                    Icon(Icons.Filled.Search, "查詢")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(state.error!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            } else if (state.result != null) {
                val r = state.result!!
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("商品資訊", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Primary)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("S/N", r.serialNumber)
                        InfoRow("SKU", r.skuCode)
                        InfoRow("商品名稱", r.productName)
                        InfoRow("品牌", r.brand)
                        InfoRow("狀態", r.status)

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("雙軌保固", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Primary)
                        Spacer(Modifier.height(4.dp))
                        InfoRow("供應商保固到期", r.supplierWarrantyExpiry ?: "--")
                        InfoRow("客戶保固到期", r.customerWarrantyExpiry ?: "--")

                        if (r.customerName != null) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("銷售資訊", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Primary)
                            Spacer(Modifier.height(4.dp))
                            InfoRow("客戶", r.customerName)
                            InfoRow("發票", r.invoiceNumber ?: "--")
                            InfoRow("銷售日期", r.saleDate ?: "--")
                        }

                        if (r.supplierName != null) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("採購資訊", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Primary)
                            Spacer(Modifier.height(4.dp))
                            InfoRow("供應商", r.supplierName)
                            InfoRow("PO", r.poNumber ?: "--")
                            InfoRow("PO 日期", r.poDate ?: "--")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label:", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
