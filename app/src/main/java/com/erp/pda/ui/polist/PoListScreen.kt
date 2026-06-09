package com.erp.pda.ui.polist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.data.model.PurchaseOrder
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.navigation.Routes
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoListScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: PoListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            IosTopBar(
                title = "採購單列表",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "刷新", tint = IosBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Routes.CREATE_PO) },
                containerColor = IosBlue,
                contentColor = IosWhite
            ) {
                Icon(Icons.Filled.Add, "建立採購單")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ─── Filter Tabs ───
            PoFilterTabs(
                activeFilter = state.activeFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            // ─── Error banner ───
            state.error?.let { err ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ─── Content ───
            if (state.isLoading && state.purchaseOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IosBlue)
                }
            } else if (state.purchaseOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暫無採購單", style = MaterialTheme.typography.bodyLarge, color = IosSecondaryLabel)
                }
            } else {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullState
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.purchaseOrders, key = { it.id }) { po ->
                            PoRow(
                                po = po,
                                onClick = { onNavigate(Routes.RECEIVING) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Filter Tabs ───
@Composable
fun PoFilterTabs(
    activeFilter: PoFilter,
    onFilterSelected: (PoFilter) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = IosWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PoFilter.entries.forEach { filter ->
                val isSelected = filter == activeFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            filter.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IosBlue.copy(alpha = 0.15f),
                        selectedLabelColor = IosBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) IosBlue.copy(alpha = 0.3f) else IosGray4,
                        selectedBorderColor = IosBlue.copy(alpha = 0.3f),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
    HorizontalDivider(color = IosGray5, thickness = 0.5.dp)
}

// ─── PO Row ───
@Composable
fun PoRow(po: PurchaseOrder, onClick: () -> Unit) {
    val statusColor = when (po.fsmStatus) {
        "Ordered" -> IosOrange
        "Partially_Received" -> IosBlue
        "Received" -> IosGreen
        else -> Color.Gray
    }

    val statusLabel = when (po.fsmStatus) {
        "Ordered" -> "已訂購"
        "Partially_Received" -> "部分收貨"
        "Received" -> "已收貨"
        else -> po.fsmStatus
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = IosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = po.poNumber,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IosLabel
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = po.supplierName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosLabel
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = "${po.currencyCode} ${"%.2f".format(po.totalAmountHkd)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TileAmber
                    )
                    if (po.orderedAt.isNotBlank()) {
                        Text(
                            text = "  |  ${po.orderedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel
                        )
                    }
                }
            }

            // Status Badge
            PoStatusBadge(
                label = statusLabel,
                color = statusColor
            )
        }
    }
}

// ─── PO Status Badge ───
@Composable
fun PoStatusBadge(label: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
