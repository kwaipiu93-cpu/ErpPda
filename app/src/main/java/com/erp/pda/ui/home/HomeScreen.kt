package com.erp.pda.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erp.pda.data.api.SessionManager
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.navigation.Routes
import com.erp.pda.ui.theme.*

data class DashboardTile(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

val dashboardTiles = listOf(
    DashboardTile("快速結帳", Icons.Filled.PointOfSale, TileCyan, Routes.CHECKOUT),
    DashboardTile("採購收貨", Icons.Filled.Inventory, TileBlue, Routes.RECEIVING),
    DashboardTile("出貨確認", Icons.Filled.LocalShipping, TileGreen, Routes.DISPATCH),
    DashboardTile("發票查詢", Icons.Filled.Receipt, TileAmber, Routes.INVOICE_LOOKUP),
    DashboardTile("S/N 查詢", Icons.Filled.Search, TileOrange, Routes.LOOKUP),
    DashboardTile("庫存盤點", Icons.Filled.Assessment, TilePurple, Routes.STOCKTAKE),
    DashboardTile("快速查庫存", Icons.Filled.Visibility, TileTeal, Routes.STOCK_CHECK),
    DashboardTile("退貨驗收", Icons.Filled.Undo, TileRed, Routes.RETURN),
    DashboardTile("跨倉調撥", Icons.Filled.SwapHoriz, TileIndigo, Routes.TRANSFER)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scannerManager: ScannerManager,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    LaunchedEffect(Unit) {
        ScanFeedback.init()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ERP PDA", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "你好, ${SessionManager.getUserName()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        SessionManager.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "登出")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dashboardTiles) { tile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onNavigate(tile.route) },
                    colors = CardDefaults.cardColors(containerColor = tile.color)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = tile.title,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tile.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
