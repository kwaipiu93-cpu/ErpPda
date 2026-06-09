package com.erp.pda.ui.quotelist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erp.pda.data.model.InvoiceSummary
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteListScreen(
    scannerManager: ScannerManager,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToCreate: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: QuoteListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            IosTopBar(title = "報價查詢", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreate() },
                containerColor = IosBlue,
                contentColor = IosWhite
            ) {
                Icon(Icons.Filled.Add, contentDescription = "建立報價")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                        items(
                            items = state.quotations,
                            key = { it.id }
                        ) { quote ->
                            SwipeableQuoteCard(
                                quote = quote,
                                onClick = { onNavigateToDetail(quote.id) },
                                onAction = { action -> viewModel.updateQuoteStatus(quote.id, action) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS-style swipe-left to reveal action buttons based on current quote_status.
 *
 * Status transitions:
 *   Draft   → [發送] [作廢]
 *   Sent    → [接受] [退回草稿]
 *   Void    → [退回草稿]
 *   Accepted → [重開]
 */
@Composable
fun SwipeableQuoteCard(
    quote: InvoiceSummary,
    onClick: () -> Unit,
    onAction: (QuoteAction) -> Unit
) {
    val actionButtons = remember(quote.quoteStatus) {
        when (quote.quoteStatus) {
            "Draft" -> listOf(
                SwipeAction(QuoteAction.VOID, IosRed, Icons.Filled.Cancel),
                SwipeAction(QuoteAction.SEND, IosBlue, Icons.Filled.Send)
            )
            "Sent" -> listOf(
                SwipeAction(QuoteAction.REVERT_DRAFT, IosGray2, Icons.Filled.Undo),
                SwipeAction(QuoteAction.ACCEPT, IosGreen, Icons.Filled.CheckCircle)
            )
            "Void" -> listOf(
                SwipeAction(QuoteAction.REVERT_DRAFT, IosGray2, Icons.Filled.Undo)
            )
            "Accepted" -> listOf(
                SwipeAction(QuoteAction.REOPEN, IosOrange, Icons.Filled.Refresh)
            )
            else -> emptyList()
        }
    }

    val buttonWidth = 80.dp
    val totalActionsWidth = (buttonWidth * actionButtons.size)
    val actionThreshold = totalActionsWidth * 0.4f

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var isOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Action buttons behind
        if (actionButtons.isNotEmpty()) {
            Row(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IosGray5),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actionButtons.forEach { act ->
                    Box(
                        modifier = Modifier
                            .width(buttonWidth)
                            .fillMaxHeight()
                            .background(act.color)
                            .clickable {
                                onAction(act.action)
                                // Snap back
                                scope.launch {
                                    offsetX.animateTo(0f, tween(200))
                                    isOpen = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(act.icon, null, tint = IosWhite, modifier = Modifier.size(20.dp))
                            Text(
                                act.action.label,
                                color = IosWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Foreground card
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(actionButtons.isNotEmpty()) {
                    if (actionButtons.isEmpty()) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = if (-offsetX.value > actionThreshold.toPx()) {
                                -totalActionsWidth.toPx()
                            } else {
                                0f
                            }
                            scope.launch {
                                offsetX.animateTo(target, tween(200))
                                isOpen = target < 0f
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, tween(200))
                                isOpen = false
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newX = (offsetX.value + dragAmount).coerceIn(
                                    -totalActionsWidth.toPx() - 20f, 0f
                                )
                                offsetX.snapTo(newX)
                            }
                        }
                    )
                }
        ) {
            QuoteListCard(quote, onClick = {
                if (isOpen) {
                    // Close swipe first
                    scope.launch {
                        offsetX.animateTo(0f, tween(200))
                        isOpen = false
                    }
                } else {
                    onClick()
                }
            })
        }
    }
}

data class SwipeAction(
    val action: QuoteAction,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun QuoteListCard(inv: InvoiceSummary, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IosWhite)
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
                    Spacer(Modifier.width(6.dp))
                    QuoteStatusBadge(inv.quoteStatus)
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
                // Show unpaid balance if applicable
                if (inv.paymentStatus != "Paid" && inv.quoteStatus == "Accepted") {
                    PaymentStatusBadge(inv.paymentStatus)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    if (inv.quoteStatus == "Draft" || inv.quoteStatus == "Sent") "← 滑動操作" else "點擊查看 >",
                    style = MaterialTheme.typography.labelSmall,
                    color = IosGray2
                )
            }
        }
    }
}

@Composable
fun QuoteStatusBadge(status: String) {
    val color = when (status) {
        "Draft" -> IosGray2
        "Sent" -> IosBlue
        "Accepted" -> IosGreen
        "Void" -> IosRed
        "Expired" -> IosGray2
        else -> IosGray2
    }
    val label = when (status) {
        "Draft" -> "草稿"
        "Sent" -> "已發送"
        "Accepted" -> "已接受"
        "Void" -> "已作廢"
        "Expired" -> "已過期"
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

@Composable
fun PaymentStatusBadge(status: String) {
    val color = when (status) {
        "Paid" -> IosGreen
        "Partially_Paid" -> IosOrange
        "Unpaid" -> IosYellow
        else -> IosGray2
    }
    val label = when (status) {
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
