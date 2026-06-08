package com.erp.pda.ui.createquote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erp.pda.data.model.CustomerSummary
import com.erp.pda.data.model.Product
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(
    scannerManager: ScannerManager,
    viewModel: CreateQuoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWarehouses()
        scannerManager.scanResults.collect { result ->
            when (state.step) {
                QuoteStep.ITEMS -> {
                    viewModel.scanBarcode(result.code)
                    ScanFeedback.success()
                }
                QuoteStep.CUSTOMER -> {
                    viewModel.searchCustomers(result.code)
                    ScanFeedback.success()
                }
                else -> ScanFeedback.success()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("報價單") },
                navigationIcon = {
                    if (state.step != QuoteStep.CUSTOMER) {
                        IconButton(onClick = {
                            when (state.step) {
                                QuoteStep.ITEMS -> viewModel.backFromItems()
                                QuoteStep.REVIEW -> viewModel.backFromReview()
                                else -> {}
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBack, "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TileOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (state.step == QuoteStep.ITEMS && state.quoteItems.isNotEmpty()) {
                BottomTotalBar(state, viewModel)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Feedback banner
            state.feedback?.let { msg ->
                Surface(
                    Modifier.fillMaxWidth(),
                    color = if (state.feedbackError)
                        MaterialTheme.colorScheme.errorContainer
                    else Success.copy(alpha = 0.2f)
                ) {
                    Text(msg, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            when (state.step) {
                QuoteStep.CUSTOMER -> CustomerStep(state, viewModel)
                QuoteStep.ITEMS -> ItemsStep(state, viewModel)
                QuoteStep.REVIEW -> ReviewStep(state, viewModel)
                QuoteStep.DONE -> DoneStep(state, viewModel)
            }
        }
    }
}

// ─── Bottom Total Bar (items step) ───

@Composable
fun BottomTotalBar(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    val grandTotal = state.quoteItems.sumOf { it.lineTotal }
    val totalItems = state.quoteItems.sumOf { it.qty }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$totalItems 項",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    "HKD ${
                        "%,.2f".format(grandTotal)
                    }",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TileOrange
                )
            }
            Button(
                onClick = vm::goToReview,
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("提交報價", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Step 1: Customer Selection ───

@Composable
fun CustomerStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.fillMaxSize()) {
        // Section header
        SectionHeader(
            icon = Icons.Filled.Business,
            title = "顧客",
            actionLabel = "選擇客戶",
            accent = TileOrange
        )

        // Search bar
        OutlinedTextField(
            value = state.customerSearch,
            onValueChange = vm::searchCustomers,
            placeholder = { Text("掃描條碼 或 輸入客戶名...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TileOrange,
                cursorColor = TileOrange
            )
        )

        Spacer(Modifier.height(12.dp))

        // Results
        val customers = state.customerResults.ifEmpty {
            if (state.customerSearch.isBlank()) {
                // Load recent/all customers on empty
                emptyList()
            } else if (state.customerSearch.length >= 2) {
                state.customerResults // will show "無匹配"
            } else {
                emptyList()
            }
        }

        if (state.customerSearch.length >= 2 && state.customerResults.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("無匹配客戶", color = Color.Gray)
            }
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(customers) { customer ->
                CustomerCard(
                    customer = customer,
                    isSelected = state.selectedCustomer?.id == customer.id,
                    onClick = { vm.selectCustomer(customer) }
                )
            }
        }

        // Bottom button
        Box(Modifier.padding(16.dp)) {
            Button(
                onClick = vm::goToItems,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.selectedCustomer != null,
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (state.selectedCustomer != null) "下一步：加入報價項目"
                    else "請先選擇客戶",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
fun CustomerCard(customer: CustomerSummary, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TileOrange.copy(alpha = 0.08f) else Color.White
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, TileOrange)
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(TileOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    customer.companyNameZh.firstOrNull()?.toString() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = TileOrange,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    customer.companyNameZh.ifBlank { customer.companyNameEn },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (customer.contactPhone != null) {
                    Text(
                        customer.contactPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            if (customer.hasOutstanding) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Error.copy(alpha = 0.1f)
                ) {
                    Text(
                        "未付 HKD ${customer.outstandingDisplay}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Error
                    )
                }
            }
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.CheckCircle, "已選", tint = TileOrange, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ─── Step 2: Items ───

@Composable
fun ItemsStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.fillMaxSize()) {
        // Section header with customer info
        SectionHeader(
            icon = Icons.Filled.ListAlt,
            title = "項目",
            subtitle = state.selectedCustomer?.companyNameZh?.ifBlank {
                state.selectedCustomer?.companyNameEn
            },
            actionLabel = "+ 新增項目",
            accent = TileOrange,
            onAction = {} // handled by search bar
        )

        // Search / Scan bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::searchProducts,
            placeholder = { Text("掃描條碼 或 搜尋 SKU / 名稱...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TileOrange,
                cursorColor = TileOrange
            )
        )

        Spacer(Modifier.height(10.dp))

        // Search results (products to add)
        if (state.searchResults.isNotEmpty()) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.05f))
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        "搜尋結果 (${state.searchResults.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    state.searchResults.take(5).forEach { product ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.addToQuote(product) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Inventory2, null, tint = TileOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.skuCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(product.nameZh, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Icon(Icons.Filled.AddCircle, "加入", tint = Success)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Quote items list
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.quoteItems.size) { index ->
                val item = state.quoteItems[index]
                val isEditing = state.editingItemIndex == index

                QuoteItemCard(
                    item = item,
                    index = index,
                    isEditing = isEditing,
                    editingQty = state.editingQty,
                    editingPrice = state.editingPrice,
                    onToggleEdit = {
                        if (isEditing) vm.cancelEditItem()
                        else vm.startEditItem(index)
                    },
                    onQtyChange = vm::updateEditingQty,
                    onPriceChange = vm::updateEditingPrice,
                    onSaveEdit = vm::saveEditItem,
                    onCancelEdit = vm::cancelEditItem,
                    onAdjustQty = { vm.adjustQty(index, it) },
                    onRemove = { vm.removeQuoteItem(index) }
                )
            }

            // "Add item" hint card
            if (state.quoteItems.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.AddShoppingCart,
                                null,
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "掃描或搜尋商品加入報價",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteItemCard(
    item: QuoteItem,
    index: Int,
    isEditing: Boolean,
    editingQty: String,
    editingPrice: String,
    onToggleEdit: () -> Unit,
    onQtyChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onAdjustQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(Modifier.padding(14.dp)) {
            // Top row: product name + price
            Row(verticalAlignment = Alignment.Top) {
                // Number circle
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TileOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TileOrange
                    )
                }
                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        item.product.nameZh.ifBlank { item.product.skuCode },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        item.product.skuCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Price
                Text(
                    "HKD ${
                        "%,.2f".format(
                            if (item.unitPrice > 0) item.unitPrice else item.product.retailPriceHkd
                        )
                    }",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TileOrange
                )
            }

            Spacer(Modifier.height(10.dp))

            if (isEditing) {
                // Editing mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = editingQty,
                        onValueChange = onQtyChange,
                        label = { Text("數量") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = editingPrice,
                        onValueChange = onPriceChange,
                        label = { Text("單價") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancelEdit) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSaveEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("儲存") }
                }
            } else {
                // Display mode: qty controls + actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Qty controls
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF5F5F5)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onAdjustQty(-1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.Remove, "減", Modifier.size(16.dp))
                            }
                            Text(
                                "${item.qty}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onAdjustQty(1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.Add, "加", Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        "小計 HKD ${"%,.2f".format(item.lineTotal)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )

                    Spacer(Modifier.weight(1f))

                    // Edit / Delete icons
                    IconButton(onClick = onToggleEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, "編輯", tint = TileOrange, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.DeleteOutline, "刪除", tint = Error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ─── Step 3: Review ───

@Composable
fun ReviewStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    val grandTotal = state.quoteItems.sumOf { it.lineTotal }

    Column(Modifier.fillMaxSize()) {
        SectionHeader(
            icon = Icons.Filled.Visibility,
            title = "確認報價",
            accent = TileOrange
        )

        LazyColumn(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Customer card
            item {
                state.selectedCustomer?.let { c ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Business, null, tint = TileOrange, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    c.companyNameZh.ifBlank { c.companyNameEn },
                                    fontWeight = FontWeight.Bold
                                )
                                c.contactPhone?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Items
            item {
                Text("報價明細", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            items(state.quoteItems.size) { index ->
                val item = state.quoteItems[index]
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(TileOrange.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TileOrange
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.product.nameZh.ifBlank { item.product.skuCode }, fontWeight = FontWeight.Bold)
                            Text(item.product.skuCode, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("×${item.qty}", fontWeight = FontWeight.Bold)
                            Text(
                                "HKD ${"%,.2f".format(item.lineTotal)}",
                                color = TileOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Warehouse
            if (state.warehouses.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("出貨倉庫", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                items(state.warehouses) { wh ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.selectWarehouse(wh.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.selectedWarehouseId == wh.id)
                                TileOrange.copy(alpha = 0.05f) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.selectedWarehouseId == wh.id) TileOrange else Color(0xFFEEEEEE)
                        )
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.selectedWarehouseId == wh.id,
                                onClick = { vm.selectWarehouse(wh.id) },
                                colors = RadioButtonDefaults.colors(selectedColor = TileOrange)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${wh.nameZh} (${wh.nameEn})",
                                fontWeight = if (state.selectedWarehouseId == wh.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // Bottom: total + actions
        Surface(
            Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("總計", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        "HKD ${"%,.2f".format(grandTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TileOrange
                    )
                }
                OutlinedButton(
                    onClick = vm::backFromReview,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("返回") }
                Button(
                    onClick = vm::submitQuotation,
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (state.isLoading)
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else
                        Text("提交報價", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Step 4: Done ───

@Composable
fun DoneStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            null,
            modifier = Modifier.size(80.dp),
            tint = Success
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "報價建立完成！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("報價單號", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    state.resultQuoteNumber ?: "--",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = TileOrange
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "金額: HKD ${"%,.2f".format(state.resultTotal)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = vm::newQuotation,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("建立新報價", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

// ─── Reusable Section Header ───

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    accent: Color = TileOrange,
    onAction: (() -> Unit)? = null
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.06f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = accent)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    HorizontalDivider(color = accent.copy(alpha = 0.1f))
}
