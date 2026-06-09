package com.erp.pda.ui.invoicelist

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
import com.erp.pda.data.model.InvoiceSummary
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.navigation.Routes
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    salesOnly: Boolean = false,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: InvoiceListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Apply salesOnly mode
    LaunchedEffect(salesOnly) {
        viewModel.setSalesOnly(salesOnly)
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            IosTopBar(
                title = if (salesOnly) "銷售訂單" else "發票列表",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "刷新", tint = IosBlue)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ─── Filter Tabs ───
            InvoiceFilterTabs(
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
            if (state.isLoading && state.invoices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IosBlue)
                }
            } else if (state.invoices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暫無發票", style = MaterialTheme.typography.bodyLarge, color = IosSecondaryLabel)
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
                        items(state.invoices, key = { it.id }) { inv ->
                            InvoiceRow(
                                invoice = inv,
                                onClick = { onNavigate(Routes.INVOICE_LOOKUP) }
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
fun InvoiceFilterTabs(
    activeFilter: InvoiceFilter,
    onFilterSelected: (InvoiceFilter) -> Unit
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
            InvoiceFilter.entries.forEach { filter ->
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

// ─── Invoice Row ───
@Composable
fun InvoiceRow(invoice: InvoiceSummary, onClick: () -> Unit) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = IosLabel
                    )
                    Spacer(Modifier.width(8.dp))
                    // Document type chip
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = TileAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = invoice.documentType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TileAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = invoice.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosLabel
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = "HKD ${"%.2f".format(invoice.grandTotalHkd)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TileAmber
                    )
                    if (invoice.issueDate.isNotBlank()) {
                        Text(
                            text = "  |  ${invoice.issueDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel
                        )
                    }
                }
            }

            // Payment Status Badge
            PaymentStatusBadge(status = invoice.paymentStatus)
        }
    }
}

// ─── Payment Status Badge ───
@Composable
fun PaymentStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "Paid" -> Triple(Success.copy(alpha = 0.12f), Success, "已付")
        "Partially_Paid" -> Triple(Warning.copy(alpha = 0.12f), Warning, "部分已付")
        "Unpaid" -> Triple(Error.copy(alpha = 0.12f), Error, "未付")
        else -> Triple(Color.Gray.copy(alpha = 0.12f), Color.Gray, status)
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
