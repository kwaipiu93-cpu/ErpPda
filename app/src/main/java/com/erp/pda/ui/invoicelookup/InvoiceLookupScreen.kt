package com.erp.pda.ui.invoicelookup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.data.model.InvoiceDetailItem
import com.erp.pda.data.model.InvoiceSummary
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import com.erp.pda.ui.components.IosTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceLookupScreen(
    scannerManager: ScannerManager,
    viewModel: InvoiceLookupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            ScanFeedback.success()
            viewModel.searchInvoices(result.code)
        }
    }

    Scaffold(
        topBar = {
            IosTopBar(
                title = if (state.selectedInvoice != null) "發票詳情" else "發票查詢",
                onBack = if (state.selectedInvoice != null) viewModel::clearSelection else null
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.selectedInvoice == null) {
                // ─── Search ───
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::searchInvoices,
                        label = { Text("掃描或輸入發票號碼") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.searchInvoices(state.searchQuery) }) {
                        Icon(Icons.Filled.Search, "查詢")
                    }
                }

                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.invoices) { inv ->
                            InvoiceCard(inv, onClick = { viewModel.selectInvoice(inv) })
                        }
                    }
                }
            } else {
                // ─── Detail ───
                val inv = state.selectedInvoice!!
                LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                    // Header
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TileAmber.copy(alpha = 0.1f))) {
                            Column(Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    Text(inv.documentType, style = MaterialTheme.typography.labelMedium, color = TileAmber)
                                }
                                Text("客戶: ${inv.customerName}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Amount summary
                    item {
                        Spacer(Modifier.height(8.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("總額", fontWeight = FontWeight.Bold)
                                    Text("HKD ${"%.2f".format(inv.grandTotalHkd)}", fontWeight = FontWeight.Bold, color = TileAmber)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("已付")
                                    Text("HKD ${"%.2f".format(inv.paidAmountHkd)}", color = Success)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("未付")
                                    val unpaid = inv.grandTotalHkd - inv.paidAmountHkd
                                    Text("HKD ${"%.2f".format(unpaid)}", color = if (unpaid > 0) Error else Success, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("折扣")
                                    Text("HKD ${"%.2f".format(inv.discountAmount)}")
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("運費")
                                    Text("HKD ${"%.2f".format(inv.deliveryCharge)}")
                                }
                            }
                        }
                    }

                    // Status badges
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip("付款", inv.paymentStatus)
                            StatusChip("物流", inv.shippingStatus)
                            StatusChip("生命週期", inv.lifecycleStatus)
                        }
                    }

                    // Dates
                    item {
                        Spacer(Modifier.height(8.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                DetailRow("開單日期", inv.issueDate)
                                DetailRow("到期日", inv.dueDate)
                                inv.notes?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text("備註: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Line items
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("明細 (${inv.items.size} 項)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileAmber)
                    }
                    items(inv.items) { item ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.skuCode, fontWeight = FontWeight.Bold)
                                    Text(item.productName, style = MaterialTheme.typography.bodySmall)
                                    Row {
                                        Text("x${item.qty}", style = MaterialTheme.typography.bodySmall)
                                        if (item.qtyShipped > 0) {
                                            Text(" (已出: ${item.qtyShipped})", style = MaterialTheme.typography.labelSmall, color = Success)
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("@${"%.2f".format(item.unitPrice)}", style = MaterialTheme.typography.bodySmall)
                                    Text("HKD ${"%.2f".format(item.lineTotalHkd)}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(inv: InvoiceSummary, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(inv.invoiceNumber, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text(inv.documentType) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = TileAmber.copy(alpha = 0.15f)))
                }
                Text("${inv.customerName} | ${inv.issueDate}", style = MaterialTheme.typography.bodySmall)
                Text("HKD ${"%.2f".format(inv.grandTotalHkd)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TileAmber)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PaymentStatusBadge(inv.paymentStatus)
                Text("點擊查看 >", style = MaterialTheme.typography.labelSmall, color = TileAmber)
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(status: String) {
    val color = when (status) {
        "Paid" -> Success
        "Partially_Paid" -> Warning
        "Unpaid" -> Error
        else -> Color.Gray
    }
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(status, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusChip(label: String, status: String) {
    val color = when (status) {
        "Paid", "Fully_Shipped", "Active" -> Success
        "Partially_Paid", "Partially_Shipped" -> Warning
        "Unpaid", "Not_Shipped" -> Error
        "Cancelled", "Closed" -> Color.Gray
        else -> Color.Gray
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
