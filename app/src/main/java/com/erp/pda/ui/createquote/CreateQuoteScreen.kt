package com.erp.pda.ui.createquote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                    viewModel.clearFeedback()
                }
                QuoteStep.CUSTOMER -> {
                    viewModel.searchCustomers(result.code)
                    ScanFeedback.success()
                    viewModel.clearFeedback()
                }
                else -> {
                    ScanFeedback.success()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.step) {
                            QuoteStep.CUSTOMER -> "建立報價 - 選擇客戶"
                            QuoteStep.ITEMS -> "建立報價 - 報價項目"
                            QuoteStep.REVIEW -> "建立報價 - 確認提交"
                            QuoteStep.DONE -> "建立報價 ✅"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TileOrange,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Feedback banner
            state.feedback?.let { msg ->
                Surface(
                    Modifier.fillMaxWidth(),
                    color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer
                        else Success.copy(alpha = 0.2f)
                ) {
                    Text(msg, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            // Step indicator (hidden on DONE)
            if (state.step != QuoteStep.DONE) {
                QuoteStepIndicator(state.step)
            }

            // Step content
            when (state.step) {
                QuoteStep.CUSTOMER -> CustomerStep(state, viewModel)
                QuoteStep.ITEMS -> ItemsStep(state, viewModel)
                QuoteStep.REVIEW -> ReviewStep(state, viewModel)
                QuoteStep.DONE -> DoneStep(state, viewModel)
            }
        }
    }
}

@Composable
fun QuoteStepIndicator(step: QuoteStep) {
    val steps = listOf(
        "客戶" to QuoteStep.CUSTOMER,
        "項目" to QuoteStep.ITEMS,
        "確認" to QuoteStep.REVIEW
    )
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        steps.forEach { (label, s) ->
            val active = step.ordinal >= s.ordinal
            val current = step == s
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (current) TileOrange
                        else if (active) TileOrange.copy(alpha = 0.3f)
                        else Color.LightGray,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${s.ordinal + 1}",
                            color = if (active) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) TileOrange else Color.Gray
                )
            }
        }
    }
}

// ─── Step 1: Customer ───

@Composable
fun CustomerStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = state.customerSearch,
            onValueChange = vm::searchCustomers,
            label = { Text("搜尋客戶 (掃描或輸入)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        if (state.customerResults.isEmpty() && state.customerSearch.length >= 2) {
            Text(
                "無匹配客戶",
                Modifier.padding(16.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            items(state.customerResults) { c ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { vm.selectCustomer(c) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.selectedCustomer?.id == c.id)
                            TileOrange.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.companyNameZh.ifBlank { c.companyNameEn },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${c.customerType} | ${c.contactPhone}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (c.hasOutstanding) {
                                Text(
                                    "未付: HKD ${c.outstandingDisplay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Error
                                )
                            }
                        }
                        if (state.selectedCustomer?.id == c.id)
                            Icon(Icons.Filled.CheckCircle, "已選", tint = Success)
                    }
                }
                HorizontalDivider()
            }
        }

        Button(
            onClick = vm::goToItems,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = state.selectedCustomer != null,
            colors = ButtonDefaults.buttonColors(containerColor = TileOrange)
        ) {
            Text(
                if (state.selectedCustomer != null)
                    "下一步: 加入報價項目"
                else "請先選擇客戶"
            )
        }
    }
}

// ─── Step 2: Quote Items ───

@Composable
fun ItemsStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::searchProducts,
            label = { Text("掃描條碼 或 搜尋商品") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        // Quote items list
        val totalItems = state.quoteItems.sumOf { it.qty }
        Text(
            "報價項目 ($totalItems 件)",
            Modifier.padding(12.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(Modifier.weight(1f)) {
            items(state.quoteItems.size) { index ->
                val item = state.quoteItems[index]
                val isEditing = state.editingItemIndex == index

                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                    if (isEditing) {
                        // Editing mode
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${item.product.skuCode} - ${item.product.nameZh}",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = state.editingQty,
                                    onValueChange = vm::updateEditingQty,
                                    label = { Text("數量") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = state.editingPrice,
                                    onValueChange = vm::updateEditingPrice,
                                    label = { Text("單價") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = vm::cancelEditItem) {
                                    Text("取消")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = vm::saveEditItem,
                                    colors = ButtonDefaults.buttonColors(containerColor = TileOrange)
                                ) {
                                    Text("儲存")
                                }
                            }
                        }
                    } else {
                        // Display mode
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.product.skuCode, fontWeight = FontWeight.Bold)
                                Text(
                                    item.product.nameZh,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "x${item.qty} | 單價: HKD ${
                                        "%.2f".format(item.unitPrice)
                                    } | 小計: HKD ${
                                        "%.2f".format(item.lineTotal)
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TileOrange
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { vm.adjustQty(index, -1) },
                                    Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, "減", Modifier.size(18.dp))
                                }
                                Text("${item.qty}", fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { vm.adjustQty(index, 1) },
                                    Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, "加", Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { vm.startEditItem(index) }) {
                                Icon(Icons.Filled.Edit, "編輯", tint = TileOrange)
                            }
                            IconButton(onClick = { vm.removeQuoteItem(index) }) {
                                Icon(Icons.Filled.Delete, "刪除", tint = Error)
                            }
                        }
                    }
                }
            }

            // Search results
            if (state.searchResults.isNotEmpty() && state.quoteItems.isEmpty()) {
                item {
                    Text(
                        "搜尋結果:",
                        Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.searchResults) { product ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clickable { vm.addToQuote(product) }
                    ) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(product.skuCode, fontWeight = FontWeight.Bold)
                                Text(
                                    product.nameZh,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Filled.AddCircle, "加入", tint = Success)
                        }
                    }
                }
            }
        }

        // Bottom navigation
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = vm::backFromItems,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回")
            }
            Button(
                onClick = vm::goToReview,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = state.quoteItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange)
            ) {
                Text("下一步: 確認報價 ($totalItems 項)")
            }
        }
    }
}

// ─── Step 3: Review ───

@Composable
fun ReviewStep(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // Customer info
        state.selectedCustomer?.let { c ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TileOrange.copy(alpha = 0.1f)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "客戶: ${c.companyNameZh.ifBlank { c.companyNameEn }}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("${c.contactPhone}")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Items summary
        Text("報價明細", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(state.quoteItems.size) { index ->
                val item = state.quoteItems[index]
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.product.skuCode, fontWeight = FontWeight.Bold)
                            Text(
                                item.product.nameZh,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "x${item.qty}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "HKD ${
                                    "%.2f".format(item.lineTotal)
                                }",
                                color = TileOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        // Total
        val grandTotal = state.quoteItems.sumOf { it.lineTotal }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TileOrange.copy(alpha = 0.1f)
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "總計",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "HKD ${"%.2f".format(grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TileOrange
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Warehouse selection
        Text("出貨倉庫", fontWeight = FontWeight.Bold)
        state.warehouses.forEach { wh ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.selectWarehouse(wh.id) }.padding(8.dp)
            ) {
                RadioButton(
                    selected = state.selectedWarehouseId == wh.id,
                    onClick = { vm.selectWarehouse(wh.id) }
                )
                Text("${wh.nameZh} (${wh.nameEn})")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Navigation
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = vm::backFromReview,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回修改")
            }
            Button(
                onClick = vm::submitQuotation,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange)
            ) {
                if (state.isLoading)
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                else
                    Text("提交報價")
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
        Spacer(Modifier.height(24.dp))
        Text(
            "報價建立完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "報價單號",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    state.resultQuoteNumber ?: "--",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = TileOrange
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "金額: HKD ${
                        "%.2f".format(state.resultTotal)
                    }",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = vm::newQuotation,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TileOrange)
        ) {
            Text("建立新報價")
        }
    }
}
