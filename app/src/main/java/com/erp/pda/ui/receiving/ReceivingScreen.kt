package com.erp.pda.ui.receiving

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.data.model.PurchaseOrder
import com.erp.pda.ErpApplication
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivingScreen(
    scannerManager: ScannerManager,
    viewModel: ReceivingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // 收集掃描結果
    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            if (state.selectedPO != null) {
                val ok = viewModel.scanSerial(result.code)
                if (ok) ScanFeedback.success()
                else ScanFeedback.error(ErpApplication.instance)
                viewModel.clearFeedback()
            }
        }
    }

    // 載入 PO 列表
    LaunchedEffect(Unit) {
        viewModel.loadPurchaseOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.selectedPO != null) "採購收貨" else "選擇採購單") },
                navigationIcon = {
                    if (state.selectedPO != null) {
                        IconButton(onClick = viewModel::backToList) {
                            Icon(Icons.Filled.ArrowBack, "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Feedback bar
            state.lastFeedback?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (state.feedbackIsError)
                        MaterialTheme.colorScheme.errorContainer
                    else Success.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.selectedPO == null) {
                // ─── PO List ───
                if (state.isLoadingPOs) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.purchaseOrders) { po ->
                            PoListItem(po = po, onClick = { viewModel.selectPO(po) })
                        }
                    }
                }
            } else {
                // ─── Receiving Detail ───
                if (state.isLoadingDetail) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val po = state.selectedPO!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // PO Info header — enhanced
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = po.poNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    AssistChip(onClick = {}, label = { Text(po.fsmStatus) })
                                }
                                Text("供應商: ${po.supplierName}", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("狀態: ${po.fsmStatus}", style = MaterialTheme.typography.bodySmall)
                                    Text("${state.items.size} 項商品", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 掃描中商品（當前選中）
                        state.items.getOrNull(state.selectedItemIndex)?.let { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .background(Color(0xFFFFF8E1))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🔍 正在掃描: ${item.poItem.skuCode}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.poItem.productName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (item.poItem.isSerialTracked) {
                                        Text(
                                            text = "保固: ${item.poItem.warrantyMonths}個月",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary
                                        )
                                    }
                                    Text(
                                        text = "已掃: ${item.scannedSerials.size} / ${item.remaining}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isComplete) Success else MaterialTheme.colorScheme.primary
                                    )
                                    // Scanned S/Ns
                                    if (item.scannedSerials.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        item.scannedSerials.forEach { sn ->
                                            Text(
                                                text = "✓ $sn",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Success
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 商品列表切換
                        LazyColumn(modifier = Modifier.weight(1.5f)) {
                            itemsIndexed(state.items) { index, item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectItem(index) },
                                    color = if (index == state.selectedItemIndex)
                                        Primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.poItem.skuCode,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${item.scannedSerials.size}/${item.remaining} 已掃",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (item.isComplete) {
                                            Text("✅", style = MaterialTheme.typography.titleLarge)
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }

                        // Submit button
                        Button(
                            onClick = viewModel::submitReceive,
                            enabled = !state.isSubmitting && state.items.all { it.isComplete },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("確認收貨")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PoListItem(po: PurchaseOrder, onClick: () -> Unit) {
    val statusColor = when (po.fsmStatus) {
        "Ordered" -> Primary
        "Partially_Received" -> Warning
        "Received" -> Success
        else -> Color.Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(po.poNumber, fontWeight = FontWeight.Bold)
                Text(po.supplierName, style = MaterialTheme.typography.bodySmall)
                Row {
                    Text("${po.currencyCode} ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "%.2f".format(po.totalAmountHkd),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = statusColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                    Text(po.fsmStatus, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text("點擊收貨 >", style = MaterialTheme.typography.labelSmall, color = Primary)
            }
        }
    }
}
