package com.erp.pda.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
            IosListItem("建立採購單", "向供應商下單", Icons.Filled.AddShoppingCart, IosBlue, Routes.CREATE_PO),
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
            IosListItem("建立報價", "新增客戶報價單", Icons.Filled.Description, IosOrange, Routes.CREATE_QUOTE),
            IosListItem("快速結帳", "B2C 一鍵現結", Icons.Filled.PointOfSale, IosMint, Routes.CHECKOUT),
            IosListItem("發票查詢", "查詢發票及明細", Icons.Filled.Receipt, IosYellow, Routes.INVOICE_LOOKUP),
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
