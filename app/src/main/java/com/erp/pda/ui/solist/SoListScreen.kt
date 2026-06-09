package com.erp.pda.ui.solist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erp.pda.data.model.SalesOrderSummary
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoListScreen(
    onNavigateToDetail: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SoListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            IosTopBar(
                title = "銷售訂單",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "刷新", tint = IosBlue)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IosBlue)
                }
            } else if (state.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = IosRed, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = IosRed)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::loadOrders) {
                            Text("重試", color = IosBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (state.orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Assignment, null, tint = IosGray2, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("暫無銷售訂單", color = IosSecondaryLabel)
                        Spacer(Modifier.height(4.dp))
                        Text("接受報價單後會自動產生", color = IosGray2, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullState
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.orders, key = { it.id }) { so ->
                            SoCard(so, onClick = { onNavigateToDetail(so.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoCard(so: SalesOrderSummary, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = IosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(so.soNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = IosLabel)
                    Spacer(Modifier.width(8.dp))
                    SoStatusBadge(so.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    so.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosLabel
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        "HKD ${"%.2f".format(so.totalAmountHkd)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IosOrange
                    )
                    so.confirmedAt?.let {
                        Text("  |  $it", style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
                    }
                }
            }
            Text("點擊查看 >", style = MaterialTheme.typography.labelSmall, color = IosGray2)
        }
    }
}

@Composable
fun SoStatusBadge(status: String) {
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
        Text(
            label,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
