package com.erp.pda.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.data.model.StockTransfer
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import com.erp.pda.ui.components.IosTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    scannerManager: ScannerManager,
    viewModel: TransferViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTransfers()
        scannerManager.scanResults.collect { result ->
            if (state.selectedST != null) {
                val ok = viewModel.scanSerial(result.code)
                if (ok) ScanFeedback.success() else ScanFeedback.error(ErpApplication.instance)
                viewModel.clearFeedback()
            }
        }
    }

    Scaffold(
        topBar = {
            IosTopBar(
                title = if (state.selectedST != null) "調撥接收" else "在途調撥單",
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

            if (state.selectedST == null) {
                // ─── List Mode ───
                if (state.isLoadingList) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (state.transfers.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暫無在途調撥單", color = Color.Gray)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.transfers) { st ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { viewModel.selectTransfer(st) }) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(st.stNumber, fontWeight = FontWeight.Bold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(st.fromWarehouseName, color = Primary)
                                            Icon(Icons.Filled.ArrowForward, null, Modifier.size(16.dp).padding(horizontal = 4.dp), tint = Color.Gray)
                                            Text(st.toWarehouseName, color = Success)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AssistChip(onClick = {}, label = { Text(st.fsmStatus) })
                                        Text("點擊接收 >", style = MaterialTheme.typography.labelSmall, color = TileIndigo)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ─── Detail Mode ───
                val st = state.selectedST!!
                Column(Modifier.fillMaxSize()) {
                    Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = TileIndigo.copy(alpha = 0.1f))) {
                        Column(Modifier.padding(12.dp)) {
                            Text(st.stNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(st.fromWarehouseName, color = Primary)
                                Icon(Icons.Filled.ArrowForward, null, Modifier.size(18.dp).padding(horizontal = 4.dp), tint = Color.Gray)
                                Text(st.toWarehouseName, color = Success)
                            }
                            Text("狀態: ${st.fsmStatus}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    state.items.getOrNull(state.selectedItemIndex)?.let { item ->
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("🔍 ${item.item.skuCode}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(item.item.productName, style = MaterialTheme.typography.bodyMedium)
                                Text("待收: ${item.scannedSerials.size} / ${item.remaining}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isComplete) Success else TileIndigo)
                                item.scannedSerials.forEach { sn ->
                                    Text("✓ $sn", style = MaterialTheme.typography.bodySmall, color = Success)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    LazyColumn(Modifier.weight(1.5f)) {
                        itemsIndexed(state.items) { index, item ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { viewModel.selectItem(index) },
                                color = if (index == state.selectedItemIndex) TileIndigo.copy(alpha = 0.08f) else Color.Transparent
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.item.skuCode, fontWeight = FontWeight.Bold)
                                        Text("${item.scannedSerials.size}/${item.remaining} 已接收",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (item.isComplete) Text("✅")
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    Button(
                        onClick = viewModel::confirmReceive,
                        enabled = !state.isSubmitting && state.items.isNotEmpty() && state.items.all { it.isComplete },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TileIndigo)
                    ) {
                        if (state.isSubmitting) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        else Text("確認接收調撥")
                    }
                }
            }
        }
    }
}
