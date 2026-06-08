package com.erp.pda.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.data.api.SessionManager
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.navigation.Routes
import com.erp.pda.ui.theme.*

data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val route: String
)

@Composable
fun HomeScreen(
    scannerManager: ScannerManager,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    LaunchedEffect(Unit) {
        ScanFeedback.init()
    }

    val userName = SessionManager.getUserName()

    val quickActions = listOf(
        QuickAction("建立報價", "新增客戶報價單", { QuickIcon(Icons.Filled.Description, IosOrange) }, Routes.CREATE_QUOTE),
        QuickAction("快速結帳", "B2C 一鍵現結", { QuickIcon(Icons.Filled.PointOfSale, IosMint) }, Routes.CHECKOUT),
        QuickAction("採購收貨", "掃碼收貨入庫", { QuickIcon(Icons.Filled.Inventory, IosBlue) }, Routes.RECEIVING),
        QuickAction("出貨確認", "掃碼出貨簽收", { QuickIcon(Icons.Filled.LocalShipping, IosGreen) }, Routes.DISPATCH),
        QuickAction("庫存盤點", "凍結倉庫盤點", { QuickIcon(Icons.Filled.Assessment, IosPurple) }, Routes.STOCKTAKE),
        QuickAction("客戶管理", "客戶主檔管理", { QuickIcon(Icons.Filled.People, IosTeal) }, Routes.CUSTOMERS),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "你好, $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = IosLabel
                    )
                    Text(
                        text = "ERP 管理系統",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryLabel
                    )
                }
                IconButton(onClick = {
                    SessionManager.clearAll()
                    onLogout()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "登出",
                        tint = IosRed.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ── Summary Cards ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "今日訂單",
                    value = "—",
                    icon = { Icon(Icons.Filled.Receipt, null, tint = IosBlue, modifier = Modifier.size(24.dp)) },
                    color = IosBlue
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "待收貨",
                    value = "—",
                    icon = { Icon(Icons.Filled.Inventory, null, tint = IosOrange, modifier = Modifier.size(24.dp)) },
                    color = IosOrange
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "待出貨",
                    value = "—",
                    icon = { Icon(Icons.Filled.LocalShipping, null, tint = IosGreen, modifier = Modifier.size(24.dp)) },
                    color = IosGreen
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "庫存項目",
                    value = "—",
                    icon = { Icon(Icons.Filled.Warehouse, null, tint = IosPurple, modifier = Modifier.size(24.dp)) },
                    color = IosPurple
                )
            }
        }

        // ── Section: 常用功能 ──
        item {
            Text(
                text = "常用功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(quickActions) { action ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate(action.route) },
                shape = MaterialTheme.shapes.medium,
                color = IosWhite
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    action.icon()
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, fontWeight = FontWeight.Medium, color = IosLabel)
                        Text(action.subtitle, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = IosGray2, modifier = Modifier.size(20.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = IosWhite
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = IosLabel)
        }
    }
}

@Composable
fun QuickIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}
