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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.*
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/* ─── ViewModel (unchanged API; only UI restructured) ─── */

data class QuoteItem(
    val product: Product,
    var qty: Int = 1,
    var unitPrice: Double = 0.0
) {
    val lineTotal: Double get() = qty * unitPrice
}

data class CreateQuoteUiState(
    // Customer
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerSummary? = null,
    val showCustomerPicker: Boolean = false,
    // Items
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val quoteItems: List<QuoteItem> = emptyList(),
    val editingItemIndex: Int = -1,
    val editingQty: String = "",
    val editingPrice: String = "",
    // Warehouse
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int = 1,
    // Status
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    // Result
    val resultQuoteNumber: String? = null,
    val resultTotal: Double = 0.0,
    val showDone: Boolean = false
)

/* ─── ViewModel ─── */

class CreateQuoteViewModel : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(CreateQuoteUiState())
    val state: StateFlow<CreateQuoteUiState> = _state.asStateFlow()

    fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getWarehouses()
                val whs = resp.body()?.data ?: emptyList()
                _state.value = _state.value.copy(
                    warehouses = whs,
                    selectedWarehouseId = whs.firstOrNull()?.id ?: 1
                )
            } catch (_: Exception) {}
        }
    }

    fun searchCustomers(query: String) {
        _state.value = _state.value.copy(customerSearch = query, showCustomerPicker = true)
        if (query.length < 2) {
            _state.value = _state.value.copy(customerResults = emptyList())
            return
        }
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchCustomers(query)
                _state.value = _state.value.copy(customerResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    fun selectCustomer(c: CustomerSummary) {
        _state.value = _state.value.copy(
            selectedCustomer = c,
            customerSearch = c.companyNameZh.ifBlank { c.companyNameEn },
            showCustomerPicker = false,
            customerResults = emptyList()
        )
    }

    fun openCustomerPicker() {
        _state.value = _state.value.copy(
            showCustomerPicker = true,
            customerSearch = ""
        )
    }

    fun closeCustomerPicker() {
        _state.value = _state.value.copy(showCustomerPicker = false, customerResults = emptyList())
    }

    fun searchProducts(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.length < 2) return
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(query)
                _state.value = _state.value.copy(searchResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    fun addToQuote(product: Product) {
        val s = _state.value
        val existing = s.quoteItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.qty++
            _state.value = s.copy(
                quoteItems = s.quoteItems.toList(),
                feedback = "${product.skuCode} x${existing.qty}",
                feedbackError = false
            )
        } else {
            val newItems = s.quoteItems + QuoteItem(product = product)
            _state.value = s.copy(
                quoteItems = newItems,
                feedback = "已加入: ${product.skuCode}",
                feedbackError = false,
                searchResults = emptyList(),
                searchQuery = ""
            )
        }
    }

    fun scanBarcode(code: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(code)
                val products = resp.body()?.data ?: emptyList()
                if (products.size == 1) {
                    addToQuote(products.first())
                } else if (products.isNotEmpty()) {
                    _state.value = _state.value.copy(searchResults = products, searchQuery = code)
                } else {
                    _state.value = _state.value.copy(feedback = "找不到商品: $code", feedbackError = true)
                }
            } catch (_: Exception) {}
        }
    }

    fun startEditItem(index: Int) {
        val item = _state.value.quoteItems.getOrNull(index) ?: return
        _state.value = _state.value.copy(
            editingItemIndex = index,
            editingQty = item.qty.toString(),
            editingPrice = if (item.unitPrice > 0) item.unitPrice.toString() else ""
        )
    }

    fun cancelEditItem() { _state.value = _state.value.copy(editingItemIndex = -1) }
    fun updateEditingQty(v: String) { _state.value = _state.value.copy(editingQty = v) }
    fun updateEditingPrice(v: String) { _state.value = _state.value.copy(editingPrice = v) }

    fun saveEditItem() {
        val s = _state.value
        val idx = s.editingItemIndex
        if (idx < 0) return
        val item = s.quoteItems.getOrNull(idx) ?: return
        item.qty = s.editingQty.toIntOrNull()?.coerceAtLeast(1) ?: item.qty
        item.unitPrice = s.editingPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: item.unitPrice
        _state.value = s.copy(quoteItems = s.quoteItems.toList(), editingItemIndex = -1)
    }

    fun adjustQty(index: Int, delta: Int) {
        val s = _state.value
        val item = s.quoteItems.getOrNull(index) ?: return
        item.qty = (item.qty + delta).coerceAtLeast(1)
        _state.value = s.copy(quoteItems = s.quoteItems.toList())
    }

    fun removeQuoteItem(index: Int) {
        val items = _state.value.quoteItems.toMutableList()
        items.removeAt(index)
        _state.value = _state.value.copy(quoteItems = items, editingItemIndex = -1)
    }

    fun selectWarehouse(id: Int) {
        _state.value = _state.value.copy(selectedWarehouseId = id)
    }

    fun submitQuotation() {
        val s = _state.value
        val cust = s.selectedCustomer ?: return
        if (s.quoteItems.isEmpty()) {
            _state.value = s.copy(feedback = "請先加入報價項目", feedbackError = true)
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val items = s.quoteItems.map { qi ->
                    QuoteItemRequest(productId = qi.product.id, qty = qi.qty, unitPrice = qi.unitPrice)
                }
                val req = CreateQuotationRequest(
                    customerId = cust.id, warehouseId = s.selectedWarehouseId, items = items
                )
                val resp = ApiClient.service.createQuotation(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val data = resp.body()?.data
                    _state.value = s.copy(
                        isSubmitting = false,
                        showDone = true,
                        resultQuoteNumber = data?.invoiceNumber ?: "",
                        resultTotal = data?.grandTotalHkd ?: 0.0
                    )
                } else {
                    _state.value = s.copy(
                        isSubmitting = false,
                        feedback = resp.body()?.error?.message ?: "建立失敗",
                        feedbackError = true
                    )
                }
            } catch (e: Exception) {
                _state.value = s.copy(
                    isSubmitting = false,
                    feedback = "網絡錯誤: ${e.localizedMessage}",
                    feedbackError = true
                )
            }
        }
    }

    fun newQuotation() {
        val whs = _state.value.warehouses
        _state.value = CreateQuoteUiState(warehouses = whs, selectedWarehouseId = whs.firstOrNull()?.id ?: 1)
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}

/* ═══════════════════════════════════════════
   SCREEN — SINGLE PAGE: Customer at top, Items below
   ═══════════════════════════════════════════ */

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
            when {
                state.showCustomerPicker -> {
                    viewModel.searchCustomers(result.code)
                    ScanFeedback.success()
                }
                else -> {
                    viewModel.scanBarcode(result.code)
                    ScanFeedback.success()
                }
            }
        }
    }

    if (state.showDone) {
        DoneOverlay(state, viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("報價單") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TileOrange,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            val grandTotal = state.quoteItems.sumOf { it.lineTotal }
            if (state.quoteItems.isNotEmpty() || state.selectedCustomer != null) {
                BottomBar(state, viewModel, grandTotal)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Feedback ──
            state.feedback?.let { msg ->
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer
                        else Success.copy(alpha = 0.2f)
                    ) { Text(msg, Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
                }
            }

            // ═══ SECTION 1: 顧客 ═══
            item { CustomerSection(state, viewModel) }

            // ═══ SECTION 2: 項目 ═══
            item { ItemsSectionHeader(state, viewModel) }

            // Search results
            if (state.searchResults.isNotEmpty()) {
                item { SearchResultsCard(state, viewModel) }
            }

            // Quote items
            items(state.quoteItems.size) { index ->
                QuoteItemCard(index, state, viewModel)
            }

            // Empty state
            if (state.quoteItems.isEmpty()) {
                item { EmptyItemsHint() }
            }

            // Bottom spacer for bottom bar
            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Customer picker overlay ──
        if (state.showCustomerPicker) {
            CustomerPickerOverlay(state, viewModel)
        }
    }
}

/* ═══ SECTION 1: 顧客 ═══ */

@Composable
fun CustomerSection(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Business, null, tint = TileOrange, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("顧客", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileOrange)
            Spacer(Modifier.weight(1f))
            if (state.selectedCustomer != null) {
                TextButton(onClick = vm::openCustomerPicker) {
                    Text("更換", color = TileOrange)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (state.selectedCustomer != null) {
            // Selected customer card
            val c = state.selectedCustomer!!
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            c.companyNameZh.firstOrNull()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold, color = TileOrange,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold)
                        c.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    }
                    if (c.hasOutstanding) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Error.copy(alpha = 0.1f)) {
                            Text("未付 HKD ${c.outstandingDisplay}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = Error)
                        }
                    }
                }
            }
        } else {
            // Select customer button
            OutlinedButton(
                onClick = vm::openCustomerPicker,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TileOrange),
                border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("選擇客戶", fontWeight = FontWeight.Bold)
            }
        }
    }

    HorizontalDivider(color = Color(0xFFEEEEEE))
}

/* ═══ Customer Picker Overlay ═══ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerOverlay(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    AlertDialog(
        onDismissRequest = vm::closeCustomerPicker,
        title = { Text("選擇客戶", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.customerSearch,
                    onValueChange = vm::searchCustomers,
                    placeholder = { Text("掃描或輸入客戶名...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TileOrange, cursorColor = TileOrange)
                )
                Spacer(Modifier.height(8.dp))
                if (state.customerResults.isEmpty() && state.customerSearch.length >= 2) {
                    Text("無匹配客戶", color = Color.Gray, modifier = Modifier.padding(8.dp))
                }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(state.customerResults) { c ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { vm.selectCustomer(c) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        c.companyNameZh.firstOrNull()?.toString() ?: "?",
                                        fontWeight = FontWeight.Bold, color = TileOrange
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold)
                                    c.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = vm::closeCustomerPicker) {
                Text("取消", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/* ═══ SECTION 2: 項目 ═══ */

@Composable
fun ItemsSectionHeader(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ListAlt, null, tint = TileOrange, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("項目", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileOrange)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { /* focus search field */ }) {
                Text("+ 新增項目", color = TileOrange, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search / scan bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::searchProducts,
            placeholder = { Text("掃描條碼 或 搜尋 SKU / 名稱...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TileOrange,
                cursorColor = TileOrange
            )
        )
    }
}

/* ═══ Search Results Card ═══ */

@Composable
fun SearchResultsCard(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.2f))
    ) {
        Column {
            Text("搜尋結果 (${state.searchResults.size})",
                style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            state.searchResults.take(5).forEach { product ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.addToQuote(product) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Inventory2, null, tint = TileOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.skuCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(product.nameZh, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.Filled.AddCircle, "加入", tint = Success, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/* ═══ Quote Item Card ═══ */

@Composable
fun QuoteItemCard(index: Int, state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    val item = state.quoteItems[index]
    val isEditing = state.editingItemIndex == index
    val price = if (item.unitPrice > 0) item.unitPrice else item.product.retailPriceHkd

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(Modifier.padding(12.dp)) {
            // Top: product info + price
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TileOrange)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.product.nameZh.ifBlank { item.product.skuCode }, fontWeight = FontWeight.Bold)
                    Text(item.product.skuCode, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text("HKD ${"%,.2f".format(price)}", fontWeight = FontWeight.Bold, color = TileOrange)
            }

            Spacer(Modifier.height(10.dp))

            if (isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = state.editingQty, onValueChange = vm::updateEditingQty,
                        label = { Text("數量") }, singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = state.editingPrice, onValueChange = vm::updateEditingPrice,
                        label = { Text("單價") }, singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = vm::cancelEditItem) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = vm::saveEditItem,
                        colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                        shape = RoundedCornerShape(10.dp)) { Text("儲存") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Qty controls
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.adjustQty(index, -1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Remove, "減", Modifier.size(14.dp))
                            }
                            Text("${item.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                            IconButton(onClick = { vm.adjustQty(index, 1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Add, "加", Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("小計 HKD ${"%,.2f".format(item.lineTotal)}",
                        style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.startEditItem(index) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Edit, "編輯", tint = TileOrange, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { vm.removeQuoteItem(index) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.DeleteOutline, "刪除", tint = Error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/* ═══ Empty Hint ═══ */

@Composable
fun EmptyItemsHint() {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AddShoppingCart, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("掃描條碼或搜尋商品加入報價", color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

/* ═══ Bottom Bar ═══ */

@Composable
fun BottomBar(state: CreateQuoteUiState, vm: CreateQuoteViewModel, grandTotal: Double) {
    val totalItems = state.quoteItems.sumOf { it.qty }

    Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("$totalItems 項", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("HKD ${"%,.2f".format(grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = TileOrange)
            }

            // Warehouse selector (compact)
            if (state.warehouses.size > 1 && totalItems > 0) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        val wh = state.warehouses.find { it.id == state.selectedWarehouseId }
                        Text(wh?.nameZh?.take(3) ?: "倉", style = MaterialTheme.typography.labelSmall)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.warehouses.forEach { wh ->
                            if (state.selectedWarehouseId == wh.id) {
                                DropdownMenuItem(
                                    text = { Text("${wh.nameZh} (${wh.nameEn})") },
                                    onClick = { vm.selectWarehouse(wh.id); expanded = false },
                                    leadingIcon = { Icon(Icons.Filled.Check, null, tint = TileOrange) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("${wh.nameZh} (${wh.nameEn})") },
                                    onClick = { vm.selectWarehouse(wh.id); expanded = false }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Button(
                onClick = vm::submitQuotation,
                enabled = state.selectedCustomer != null && state.quoteItems.isNotEmpty() && !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("提交報價", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* ═══ Done Overlay ═══ */

@Composable
fun DoneOverlay(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Success)
            Spacer(Modifier.height(20.dp))
            Text("報價建立完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))) {
                Column(Modifier.padding(20.dp)) {
                    Text("報價單號", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(state.resultQuoteNumber ?: "--", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge, color = TileOrange)
                    Spacer(Modifier.height(8.dp))
                    Text("金額: HKD ${"%,.2f".format(state.resultTotal)}", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = vm::newQuotation, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange),
                shape = RoundedCornerShape(12.dp)) {
                Text("建立新報價", fontWeight = FontWeight.Bold)
            }
        }
    }
}
