package com.erp.pda.ui.sodetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoDetailScreen(
    soId: Int,
    onBack: () -> Unit = {},
    viewModel: SoDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(soId) { viewModel.loadDetail(soId) }

    Scaffold(
        topBar = {
            IosTopBar(
                title = state.detail?.soNumber ?: "銷售訂單",
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IosBlue)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = IosRed, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = IosRed)
                    }
                }
            }
            state.detail != null -> {
                val so = state.detail!!
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Card
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = IosWhite), elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(so.soNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = IosLabel)
                                    Spacer(Modifier.width(12.dp))
                                    SoDetailStatusBadge(so.status)
                                }
                                Spacer(Modifier.height(8.dp))
                                DetailRow("客戶", so.customerName)
                                DetailRow("金額", "HKD ${"%.2f".format(so.totalAmountHkd)}")
                                so.confirmedAt?.let { DetailRow("確認日期", it) }
                                so.notes?.takeIf { it.isNotBlank() }?.let { DetailRow("備註", it) }
                            }
                        }
                    }

                    // Items
                    item {
                        Text("明細 (${so.items.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = IosLabel)
                    }

                    itemsIndexed(so.items) { idx, item ->
                        Card(colors = CardDefaults.cardColors(containerColor = IosWhite), elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${idx + 1}", color = IosGray2, style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.skuCode, fontWeight = FontWeight.Bold, color = IosLabel)
                                }
                                Text(item.nameZh, color = IosSecondaryLabel, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("數量: ${item.qty}", color = IosLabel)
                                    Text("已出: ${item.qtyFulfilled}", color = if (item.qtyFulfilled >= item.qty) IosGreen else IosOrange)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("單價: ${"%.2f".format(item.unitPrice)}", color = IosSecondaryLabel)
                                    Text("小計: ${"%.2f".format(item.lineTotalHkd)}", fontWeight = FontWeight.Bold, color = IosOrange)
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
fun DetailRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", color = IosSecondaryLabel, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = IosLabel, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SoDetailStatusBadge(status: String) {
    val color = when (status) {
        "Draft" -> IosGray2
        "Confirmed" -> IosGreen
        "Cancelled" -> IosRed
        else -> IosGray2
    }
    val label = when (status) {
        "Draft" -> "草稿"
        "Confirmed" -> "已確認"
        "Cancelled" -> "已取消"
        else -> status
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}
