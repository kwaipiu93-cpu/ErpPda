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
import com.erp.pda.ui.theme.*
import com.erp.pda.ui.components.IosTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupScreen(
    scannerManager: ScannerManager,
    onBack: () -> Unit = {},
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
            IosTopBar(
                title = "S/N 查詢",
                onBack = onBack
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
                        // Product section
                        Text("📦 商品資訊", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileOrange)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("S/N", r.serialNumber)
                        InfoRow("SKU", r.skuCode)
                        InfoRow("商品名稱", r.productName)
                        InfoRow("品牌", r.brand)

                        // Status badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("狀態:", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            val statusColor = when (r.status) {
                                "Sold" -> Success
                                "In_Stock" -> Primary
                                "Allocated" -> Warning
                                "Returned" -> Color.Gray
                                "Defective" -> Error
                                else -> Color.Gray
                            }
                            Surface(color = statusColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                                Text(r.status, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Warranty section with expiry warning
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("🛡️ 雙軌保固", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileOrange)
                        Spacer(Modifier.height(4.dp))

                        WarrantyRow("供應商保固到期", r.supplierWarrantyExpiry)
                        WarrantyRow("客戶保固到期", r.customerWarrantyExpiry)

                        if (r.customerName != null) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("🏢 銷售資訊", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileGreen)
                            Spacer(Modifier.height(4.dp))
                            InfoRow("客戶", r.customerName!!)
                            InfoRow("發票", r.invoiceNumber ?: "--")
                            InfoRow("銷售日期", r.saleDate ?: "--")
                        }

                        if (r.supplierName != null) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("🏭 採購資訊", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileBlue)
                            Spacer(Modifier.height(4.dp))
                            InfoRow("供應商", r.supplierName!!)
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

@Composable
fun WarrantyRow(label: String, date: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        if (date != null) {
            // Check if expiring within 30 days
            val isExpiringSoon = try {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val expiryDate = fmt.parse(date)
                val thirtyDaysFromNow = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                expiryDate != null && expiryDate.time < thirtyDaysFromNow
            } catch (_: Exception) { false }

            Column {
                Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = if (isExpiringSoon) Error else Color.Unspecified)
                if (isExpiringSoon) {
                    Text("⚠ 即將到期", style = MaterialTheme.typography.labelSmall, color = Error)
                }
            }
        } else {
            Text("--", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
