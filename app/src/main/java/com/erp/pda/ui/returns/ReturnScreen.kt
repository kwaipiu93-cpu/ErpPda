package com.erp.pda.ui.returns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.data.model.CreditNote
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import com.erp.pda.ui.components.IosTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnScreen(
    scannerManager: ScannerManager,
    viewModel: ReturnViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCreditNotes()
        scannerManager.scanResults.collect { result ->
            if (state.selectedCN != null) {
                val ok = viewModel.scanSerial(result.code)
                if (ok) ScanFeedback.success() else ScanFeedback.error(ErpApplication.instance)
                viewModel.clearFeedback()
            }
        }
    }

    Scaffold(
        topBar = {
            IosTopBar(
                title = if (state.selectedCN != null) "退貨驗收" else "退貨單列表",
                onBack = viewModel::backToList
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.feedback?.let { msg ->
                Surface(Modifier.fillMaxWidth(), color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                    Text(msg, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            if (state.selectedCN == null) {
                // ─── List Mode ───
                if (state.isLoadingList) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (state.creditNotes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暫無待處理退貨單", color = Color.Gray)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.creditNotes) { cn ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { viewModel.selectCreditNote(cn) }) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(cn.cnNumber, fontWeight = FontWeight.Bold)
                                            if (cn.hasGoodsReturn) {
                                                Spacer(Modifier.width(8.dp))
                                                AssistChip(onClick = {}, label = { Text("實體退貨") },
                                                    colors = AssistChipDefaults.assistChipColors(containerColor = Warning.copy(alpha = 0.2f)))
                                            }
                                        }
                                        Text("${cn.customerName} | ${cn.creditType}", style = MaterialTheme.typography.bodySmall)
                                        if (cn.totalAmountHkd > 0) Text("HKD ${cn.totalAmountHkd}", style = MaterialTheme.typography.bodySmall, color = Error)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AssistChip(onClick = {}, label = { Text(cn.fsmStatus) })
                                        Text("點擊查看 >", style = MaterialTheme.typography.labelSmall, color = Primary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ─── Detail Mode ───
                val cn = state.selectedCN!!
                Column(Modifier.fillMaxSize()) {
                    // Header info card
                    Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = TileRed.copy(alpha = 0.1f))) {
                        Column(Modifier.padding(12.dp)) {
                            Text(cn.cnNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("客戶: ${cn.customerName}")
                            Text("類型: ${cn.creditType} | 狀態: ${cn.fsmStatus}")
                            if (cn.totalAmountHkd > 0) Text("金額: HKD ${cn.totalAmountHkd}", color = Error, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Current scan item
                    state.items.getOrNull(state.selectedItemIndex)?.let { item ->
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("🔍 ${item.item.skuCode}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(item.item.productName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    // Defective toggle
                                    if (item.item.isSerialTracked) {
                                        IconButton(onClick = viewModel::toggleDefective) {
                                            Icon(
                                                Icons.Filled.Warning,
                                                contentDescription = "瑕疵",
                                                tint = if (item.isDefective) Error else Color.Gray
                                            )
                                        }
                                    }
                                }
                                Text("退貨: ${item.scannedSerials.size} / ${item.item.qty}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isComplete) Success else Primary)
                                if (item.isDefective) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(color = Error.copy(alpha = 0.1f)) {
                                        Text("⚠ 瑕疵品", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall, color = Error)
                                    }
                                }
                                item.scannedSerials.forEach { sn ->
                                    Text("✓ $sn", style = MaterialTheme.typography.bodySmall, color = Success)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Item selector list
                    LazyColumn(Modifier.weight(1.5f)) {
                        itemsIndexed(state.items) { index, item ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { viewModel.selectItem(index) },
                                color = if (index == state.selectedItemIndex) TileRed.copy(alpha = 0.08f) else Color.Transparent
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.item.skuCode, fontWeight = FontWeight.Bold)
                                        Text("退貨 x${item.item.qty} | ${item.scannedSerials.size} 已掃",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (item.isDefective) Text("⚠", color = Error)
                                    if (item.isComplete) Text("✅")
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    // Confirm button
                    Button(
                        onClick = viewModel::confirmReturn,
                        enabled = !state.isSubmitting && state.items.isNotEmpty() && state.items.all { it.isComplete },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TileRed)
                    ) {
                        if (state.isSubmitting) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        else Text("確認退貨入庫")
                    }
                }
            }
        }
    }
}
