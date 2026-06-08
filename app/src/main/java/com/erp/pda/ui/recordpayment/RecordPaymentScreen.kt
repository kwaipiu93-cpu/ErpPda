package com.erp.pda.ui.recordpayment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.data.model.InvoiceSummary
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    scannerManager: ScannerManager,
    viewModel: RecordPaymentViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Scanner integration: scan invoice barcode -> search
    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            ScanFeedback.success()
            viewModel.searchInvoices(result.code)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.selectedInvoice != null) "記錄付款" else "收款記錄"
                    )
                },
                navigationIcon = {
                    if (state.selectedInvoice != null) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.ArrowBack, "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Success,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Success feedback banner
            state.successMessage?.let { msg ->
                Surface(
                    Modifier.fillMaxWidth(),
                    color = Success.copy(alpha = 0.15f)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            msg,
                            fontWeight = FontWeight.Bold,
                            color = Success,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            viewModel.dismissSuccess()
                            viewModel.resetForm()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, "關閉", tint = Success)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Error banner
            state.error?.let { err ->
                Surface(
                    Modifier.fillMaxWidth(),
                    color = Error.copy(alpha = 0.1f)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            err,
                            color = Error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.selectedInvoice == null) {
                // ─── Search & Invoice List ───
                SearchAndInvoiceList(state, viewModel)
            } else {
                // ─── Payment Form ───
                PaymentForm(state, viewModel)
            }
        }
    }
}

@Composable
private fun SearchAndInvoiceList(
    state: RecordPaymentUiState,
    viewModel: RecordPaymentViewModel
) {
    Column(Modifier.fillMaxSize()) {
        // Search bar
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::searchInvoices,
                label = { Text("掃描或輸入發票號碼") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Success,
                    focusedLabelColor = Success,
                    cursorColor = Success
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.searchInvoices(state.searchQuery) }) {
                Icon(Icons.Filled.Search, "查詢", tint = Success)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Success)
            }
        } else if (state.searchQuery.isNotBlank() && state.invoices.isEmpty() && state.error == null) {
            // No results
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("找不到相關發票", color = Color.Gray)
                }
            }
        } else {
            // Invoice list
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.invoices) { inv ->
                    InvoiceCard(inv, onClick = { viewModel.selectInvoice(inv) })
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(inv: InvoiceSummary, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(inv.invoiceNumber, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(inv.documentType) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Success.copy(alpha = 0.15f)
                        )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${inv.customerName} | ${inv.issueDate}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "HKD ${"%.2f".format(inv.grandTotalHkd)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PaymentStatusBadge(inv.paymentStatus)
                Spacer(Modifier.height(2.dp))
                Text(
                    "點擊收款 >",
                    style = MaterialTheme.typography.labelSmall,
                    color = Success
                )
            }
        }
    }
}

@Composable
private fun PaymentStatusBadge(status: String) {
    val color = when (status) {
        "Paid" -> Success
        "Partially_Paid" -> Warning
        "Unpaid" -> Error
        else -> Color.Gray
    }
    val label = when (status) {
        "Paid" -> "已付"
        "Partially_Paid" -> "部分已付"
        "Unpaid" -> "未付"
        else -> status
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentForm(
    state: RecordPaymentUiState,
    viewModel: RecordPaymentViewModel
) {
    val inv = state.selectedInvoice ?: return
    val unpaid = inv.grandTotalHkd - inv.paidAmountHkd

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Invoice header card
        item {
            Spacer(Modifier.height(8.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.08f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            inv.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            inv.documentType,
                            style = MaterialTheme.typography.labelMedium,
                            color = Success
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "客戶: ${inv.customerName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Amount summary card
        item {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    AmountRow("總額", inv.grandTotalHkd, MaterialTheme.colorScheme.onSurface)
                    AmountRow("已付", inv.paidAmountHkd, Success)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    AmountRow("未付", unpaid, if (unpaid > 0) Error else Success, bold = true)
                }
            }
        }

        // Payment amount input
        item {
            Spacer(Modifier.height(12.dp))
            Text("付款金額 (HKD)", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.paymentAmount,
                onValueChange = viewModel::updatePaymentAmount,
                label = { Text("輸入收款金額") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("HKD ") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Success,
                    focusedLabelColor = Success,
                    cursorColor = Success
                ),
                supportingText = {
                    Text("未付: HKD ${"%.2f".format(unpaid)}")
                }
            )

            // Quick-fill buttons
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.updatePaymentAmount("%.2f".format(unpaid)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Success.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("全額 HKD ${"%.2f".format(unpaid)}", color = Success, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Payment method selection
        item {
            Spacer(Modifier.height(12.dp))
            Text("付款方式", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FPS" to "轉數快", "Cash" to "現金").forEach { (method, label) ->
                        FilterChip(
                            selected = state.paymentMethod == method,
                            onClick = { viewModel.updatePaymentMethod(method) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Success.copy(alpha = 0.15f),
                                selectedLabelColor = Success
                            )
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cheque" to "支票", "Bank_Transfer" to "銀行轉帳").forEach { (method, label) ->
                        FilterChip(
                            selected = state.paymentMethod == method,
                            onClick = { viewModel.updatePaymentMethod(method) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Success.copy(alpha = 0.15f),
                                selectedLabelColor = Success
                            )
                        )
                    }
                }
            }
        }

        // Reference number
        item {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.referenceNumber,
                onValueChange = viewModel::updateReferenceNumber,
                label = { Text("參考編號 (選填)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Success,
                    focusedLabelColor = Success,
                    cursorColor = Success
                )
            )
        }

        // Submit button
        item {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = viewModel::submitPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSubmitting && (state.paymentAmount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Payments, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "確認收款 HKD ${state.paymentAmount.ifBlank { "0.00" }}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    color: Color,
    bold: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "HKD ${"%.2f".format(amount)}",
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
