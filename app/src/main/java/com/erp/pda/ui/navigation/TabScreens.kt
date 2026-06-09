package com.erp.pda.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.erp.pda.ui.theme.*

// ─── 採購 Tab ───
@Composable
fun PurchaseTab(onNavigate: (String) -> Unit) {
    IosGroupedList(
        title = "採購",
        items = listOf(
            IosListItem("採購單列表", "瀏覽及管理採購單", Icons.AutoMirrored.Filled.ListAlt, IosBlue, Routes.PO_LIST),
            IosListItem("採購收貨", "掃描 S/N 收貨入庫", Icons.Filled.Inventory, IosGreen, Routes.RECEIVING),
            IosListItem("S/N 保固查詢", "查詢上下游保固期", Icons.Filled.Search, IosOrange, Routes.LOOKUP),
        ),
        onNavigate = onNavigate
    )
}

// ─── 銷售 Tab ───
@Composable
fun SalesTab(onNavigate: (String) -> Unit) {
    IosGroupedList(
        title = "銷售",
        items = listOf(
            IosListItem("報價查詢", "瀏覽及編輯報價單", Icons.Filled.ListAlt, IosOrange, Routes.QUOTE_LIST),
            IosListItem("銷售訂單", "進行中發票", Icons.Filled.Receipt, IosBlue, Routes.SALES_ORDERS),
            IosListItem("發票列表", "全部發票紀錄", Icons.Filled.History, IosYellow, Routes.INVOICE_LIST),
            IosListItem("快速結帳", "B2C 一鍵現結", Icons.Filled.PointOfSale, IosMint, Routes.CHECKOUT),
            IosListItem("客戶管理", "管理客戶主檔", Icons.Filled.People, IosTeal, Routes.CUSTOMERS),
            IosListItem("收款記錄", "登記客戶付款", Icons.Filled.Payments, IosGreen, Routes.RECORD_PAYMENT),
        ),
        onNavigate = onNavigate
    )
}

// ─── 倉庫 Tab ───
@Composable
fun WarehouseTab(onNavigate: (String) -> Unit) {
    IosGroupedList(
        title = "倉庫",
        items = listOf(
            IosListItem("出貨確認", "掃描 S/N 出貨簽收", Icons.Filled.LocalShipping, IosGreen, Routes.DISPATCH),
            IosListItem("庫存盤點", "凍結倉庫進行盤點", Icons.Filled.Assessment, IosPurple, Routes.STOCKTAKE),
            IosListItem("快速查庫存", "即時查看庫存狀態", Icons.Filled.Visibility, IosTeal, Routes.STOCK_CHECK),
            IosListItem("退貨驗收", "客戶退貨入庫處理", Icons.AutoMirrored.Filled.Undo, IosRed, Routes.RETURN),
            IosListItem("跨倉調撥", "倉庫間調撥移轉", Icons.Filled.SwapHoriz, IosIndigo, Routes.TRANSFER),
        ),
        onNavigate = onNavigate
    )
}

// ─── 更多 Tab ───
@Composable
fun MoreTab(onNavigate: (String) -> Unit) {
    IosGroupedList(
        title = "更多",
        items = listOf(
            IosListItem("客戶管理", "查看和管理客戶", Icons.Filled.People, IosTeal, Routes.CUSTOMERS),
            IosListItem("S/N 查詢", "序號穿透查詢", Icons.Filled.Search, IosOrange, Routes.LOOKUP),
        ),
        onNavigate = onNavigate
    )
}
