package com.erp.pda.ui.quotelist

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
import com.erp.pda.data.model.InvoiceSummary
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteListScreen(
    scannerManager: ScannerManager,
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit = {},
    viewModel: QuoteListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            IosTopBar(title = "報價查詢", onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
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
                        TextButton(onClick = viewModel::loadQuotations) {
                            Text("重試", color = IosBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (state.quotations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Description, null, tint = IosGray2, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("暫無報價單", color = IosSecondaryLabel)
                    }
                }
            } else {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullState
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.quotations) { quote ->
                            QuoteListCard(quote, onClick = { onNavigateToDetail(quote.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteListCard(inv: InvoiceSummary, onClick: () -> Unit) {
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
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = IosOrange.copy(alpha = 0.12f)
                    ) {
                        Text(
                            inv.documentType,
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = IosOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${inv.customerName}  |  ${inv.issueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel
                )
                Text(
                    "HKD ${"%.2f".format(inv.grandTotalHkd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IosOrange
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuoteStatusBadge(inv.paymentStatus)
                Spacer(Modifier.height(4.dp))
                Text("點擊查看 >", style = MaterialTheme.typography.labelSmall, color = IosGray2)
            }
        }
    }
}

@Composable
fun QuoteStatusBadge(status: String) {
    val color = when (status) {
        "Paid", "Confirmed" -> IosGreen
        "Partially_Paid", "Draft" -> IosOrange
        "Unpaid", "Pending" -> IosYellow
        "Cancelled", "Closed" -> IosGray2
        else -> IosGray2
    }
    val label = when (status) {
        "Draft" -> "草稿"
        "Pending" -> "待確認"
        "Confirmed" -> "已確認"
        "Cancelled" -> "已取消"
        "Closed" -> "已關閉"
        "Paid" -> "已付款"
        "Partially_Paid" -> "部分付款"
        "Unpaid" -> "未付款"
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
