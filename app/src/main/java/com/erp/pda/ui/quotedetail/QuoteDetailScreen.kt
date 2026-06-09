package com.erp.pda.ui.quotedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erp.pda.data.model.*
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.components.IosTopBar
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    quoteId: Int,
    scannerManager: ScannerManager,
    onBack: () -> Unit,
    viewModel: QuoteDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(quoteId) {
        viewModel.loadDetail(quoteId)
    }

    Scaffold(
        topBar = {
            IosTopBar(
                title = if (state.editing) "編輯報價" else "報價詳情",
                onBack = {
                    if (state.editing) viewModel.cancelEditing()
                    else onBack()
                },
                actions = {
                    if (!state.editing && state.detail != null) {
                        IconButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Filled.Edit, "編輯", tint = IosBlue)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.editing) {
                Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = IosWhite) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val total = state.editItems.filter { !it.isDeleted }.sumOf { it.lineTotal }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${state.editItems.count { !it.isDeleted }} 項",
                                fontSize = 11.sp, color = IosSecondaryLabel
                            )
                            Text(
                                "HKD ${"%,.2f".format(total)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = IosOrange
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::cancelEditing,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("取消")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = viewModel::saveChanges,
                            enabled = state.hasChanges && !state.saving,
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    Modifier.size(20.dp),
                                    color = IosWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("儲存", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading && state.detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IosBlue)
            }
            return@Scaffold
        }

        if (state.error != null && state.detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = IosRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, color = IosRed)
                }
            }
            return@Scaffold
        }

        val detail = state.detail ?: return@Scaffold

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // ── Feedback toast ──
            state.feedback?.let { fb ->
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = if (state.feedbackError) IosRed.copy(alpha = 0.1f) else IosGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            fb,
                            Modifier.padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (state.feedbackError) IosRed else IosGreen
                        )
                    }
                }
            }

            // ═══ Header Card ═══
            item {
                Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = IosOrange.copy(alpha = 0.08f))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(detail.invoiceNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = IosOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    detail.documentType,
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IosOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        if (state.editing) {
                            // Editable customer
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.openPickCustomer() }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("客戶: ", color = IosSecondaryLabel)
                                Text(
                                    state.editCustomerName.ifBlank { detail.customerName },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Filled.Edit, null, tint = IosBlue, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Text("客戶: ${detail.customerName}", style = MaterialTheme.typography.bodyLarge, color = IosSecondaryLabel)
                        }
                    }
                }
            }

            // ═══ Amount Summary ═══
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        if (state.editing) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("總額", fontWeight = FontWeight.Bold)
                                val editTotal = state.editItems.filter { !it.isDeleted }.sumOf { it.lineTotal }
                                Text(
                                    "HKD ${"%,.2f".format(editTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    color = IosOrange
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("原總額", color = IosSecondaryLabel)
                                Text(
                                    "HKD ${"%,.2f".format(detail.grandTotalHkd)}",
                                    color = IosSecondaryLabel
                                )
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("總額", fontWeight = FontWeight.Bold)
                                Text("HKD ${"%,.2f".format(detail.grandTotalHkd)}", fontWeight = FontWeight.Bold, color = IosOrange)
                            }
                            ListDetailRow("已付", "HKD ${"%,.2f".format(detail.paidAmountHkd)}", IosGreen)
                            val unpaid = detail.grandTotalHkd - detail.paidAmountHkd
                            ListDetailRow("未付", "HKD ${"%,.2f".format(unpaid)}", if (unpaid > 0) IosRed else IosGreen)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            ListDetailRow("折扣", "HKD ${"%,.2f".format(detail.discountAmount)}")
                            ListDetailRow("運費", "HKD ${"%,.2f".format(detail.deliveryCharge)}")
                        }
                    }
                }
            }

            // ═══ Status Badges ═══
            if (!state.editing) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuoteDetailStatusChip("付款", detail.paymentStatus)
                        QuoteDetailStatusChip("物流", detail.shippingStatus)
                        QuoteDetailStatusChip("狀態", detail.lifecycleStatus)
                    }
                }
            }

            // ═══ Dates & Notes ═══
            item {
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        ListDetailRow("開單日期", detail.issueDate)
                        ListDetailRow("到期日", detail.dueDate)
                        if (state.editing) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.editNotes,
                                onValueChange = viewModel::setEditNotes,
                                label = { Text("備註") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2, maxLines = 4,
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
                            detail.notes?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("備註: $it", style = MaterialTheme.typography.bodySmall, color = IosGray2)
                            }
                        }
                    }
                }
            }

            // ═══ Warehouse (editable) ═══
            if (state.editing) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.openPickWarehouse() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("倉庫", color = IosSecondaryLabel, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    state.editWarehouseName.ifBlank { "點擊選擇倉庫" },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(Icons.Filled.Edit, null, tint = IosBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ═══ Line Items ═══
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.editing) "明細 (${state.editItems.size} 項)" else "明細 (${detail.items.size} 項)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = IosOrange,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            if (state.editing) {
                // ── Add item button ──
                item {
                    TextButton(
                        onClick = viewModel::openPickProduct,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, tint = IosBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("加入項目", color = IosBlue, fontWeight = FontWeight.Bold)
                    }
                }

                itemsIndexed(state.editItems) { idx, item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .then(if (item.isDeleted) Modifier else Modifier)
                    ) {
                        Row(
                            Modifier
                                .padding(12.dp)
                                .then(
                                    if (item.isDeleted) Modifier.background(IosRed.copy(alpha = 0.05f))
                                    else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.skuCode,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isDeleted) IosGray2 else IosLabel
                                    )
                                    if (item.isModified && !item.isDeleted) {
                                        Spacer(Modifier.width(4.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = IosOrange.copy(alpha = 0.15f)) {
                                            Text(
                                                "已修改",
                                                Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                fontSize = 10.sp,
                                                color = IosOrange
                                            )
                                        }
                                    }
                                }
                                Text(
                                    item.productName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.isDeleted) IosGray2 else IosSecondaryLabel
                                )
                                if (state.editItemIdx == idx) {
                                    // ═══ Edit Item Dialog ═══
                                    AlertDialog(
                                        onDismissRequest = viewModel::cancelEditItem,
                                        title = { Text("編輯項目", fontWeight = FontWeight.Bold) },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${item.skuCode} - ${item.productName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                                }
                                                OutlinedTextField(
                                                    value = state.editItemQty,
                                                    onValueChange = viewModel::setEditItemQty,
                                                    label = { Text("數量") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = state.editItemPrice,
                                                    onValueChange = viewModel::setEditItemPrice,
                                                    label = { Text("單價 (HKD)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = state.editItemDesc,
                                                    onValueChange = viewModel::setEditItemDesc,
                                                    label = { Text("描述（可選）") },
                                                    singleLine = true,
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = viewModel::saveEditItem,
                                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("確認") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = viewModel::cancelEditItem) { Text("取消", color = IosGray2) }
                                        },
                                        containerColor = IosWhite,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Row {
                                        IconButton(
                                            onClick = { viewModel.adjItemQty(idx, -1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Remove, null, tint = IosGray2, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            "x${item.qty}",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                        IconButton(
                                            onClick = { viewModel.adjItemQty(idx, 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Add, null, tint = IosBlue, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "@${"%.2f".format(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.isDeleted) IosGray2 else IosSecondaryLabel
                                )
                                Text(
                                    "HKD ${"%.2f".format(item.lineTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isDeleted) IosGray2 else IosLabel
                                )
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    IconButton(
                                        onClick = { viewModel.startEditItem(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, null, tint = IosBlue, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleDeleteItem(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            if (item.isDeleted) Icons.Filled.Undo else Icons.Filled.Delete,
                                            null,
                                            tint = if (item.isDeleted) IosGray2 else IosRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── View mode items ──
                items(detail.items) { item ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.skuCode, fontWeight = FontWeight.Bold)
                                Text(item.productName, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
                                Row {
                                    Text("x${item.qty}", style = MaterialTheme.typography.bodySmall)
                                    if (item.qtyShipped > 0) {
                                        Text(
                                            " (已出: ${item.qtyShipped})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IosGreen
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("@${"%.2f".format(item.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
                                Text("HKD ${"%.2f".format(item.lineTotalHkd)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // ── Customer Picker Dialog ──
    if (state.pickingCustomer) {
        AlertDialog(
            onDismissRequest = viewModel::closePickCustomer,
            title = { Text("選擇客戶", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.customerSearch,
                        onValueChange = viewModel::searchCustomer,
                        placeholder = { Text("搜尋客戶...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = IosGray2) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.customerResults.isEmpty() && state.customerSearch.length >= 2) {
                        Text("無匹配結果", color = IosSecondaryLabel, modifier = Modifier.padding(8.dp))
                    }
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(state.customerResults) { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.selectCustomer(c) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(IosOrange.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        c.companyNameZh.firstOrNull()?.toString() ?: "?",
                                        fontWeight = FontWeight.Bold, color = IosOrange, fontSize = 14.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    c.contactPhone?.let { Text(it, fontSize = 12.sp, color = IosSecondaryLabel) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closePickCustomer) { Text("取消", color = IosGray2) } },
            containerColor = IosWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Warehouse Picker Dialog ──
    if (state.pickingWarehouse) {
        AlertDialog(
            onDismissRequest = viewModel::closePickWarehouse,
            title = { Text("選擇倉庫", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(state.warehouseResults) { w ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.selectWarehouse(w) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warehouse, null, tint = IosBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(w.nameZh.ifBlank { w.nameEn }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closePickWarehouse) { Text("取消", color = IosGray2) } },
            containerColor = IosWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Product Picker Dialog ──
    if (state.pickingProduct && !state.creatingCustomItem) {
        AlertDialog(
            onDismissRequest = viewModel::closePickProduct,
            title = { Text("加入商品", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.productSearch,
                        onValueChange = viewModel::searchProduct,
                        placeholder = { Text("搜尋 SKU 或商品名稱...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = IosGray2) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.productResults.isNotEmpty()) {
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(state.productResults) { p ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { viewModel.addProduct(p) }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Inventory2, null, tint = IosBlue, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.skuCode, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(p.nameZh, fontSize = 12.sp, color = IosSecondaryLabel)
                                    }
                                    Text(
                                        "HKD ${"%.2f".format(p.retailPriceHkd)}",
                                        fontSize = 12.sp,
                                        color = IosOrange
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Filled.AddCircle, null, tint = IosGreen, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    } else if (state.productSearch.isNotBlank()) {
                        Text("無匹配商品", color = IosSecondaryLabel, modifier = Modifier.padding(8.dp))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TextButton(onClick = viewModel::startCustomItem, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.NoteAdd, null, tint = IosBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("手動新增項目", color = IosBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closePickProduct) { Text("取消", color = IosGray2) } },
            containerColor = IosWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Custom Item Dialog ──
    if (state.creatingCustomItem) {
        AlertDialog(
            onDismissRequest = viewModel::cancelCustomItem,
            title = { Text("手動新增項目", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.customItemName,
                        onValueChange = viewModel::setCustomName,
                        label = { Text("項目名稱 *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = state.customItemPrice,
                        onValueChange = viewModel::setCustomPrice,
                        label = { Text("單價 (HKD)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::submitCustomItem,
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("加入")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelCustomItem) { Text("返回") } },
            containerColor = IosWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ListDetailRow(label: String, value: String, valueColor: Color = IosLabel) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = IosSecondaryLabel)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun QuoteDetailStatusChip(label: String, status: String) {
    val color = when (status) {
        "Paid", "Fully_Shipped", "Active" -> IosGreen
        "Partially_Paid", "Partially_Shipped" -> IosOrange
        "Unpaid", "Not_Shipped" -> IosRed
        "Cancelled", "Closed" -> IosGray2
        else -> IosGray2
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = IosGray2)
            Text(status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
