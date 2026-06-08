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

/* ─── Data classes ─── */

data class QuoteItem(
    val product: Product,
    var qty: Int = 1,
    var unitPrice: Double = 0.0
) {
    val lineTotal: Double get() = qty * unitPrice
}

data class CreateQuoteUiState(
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerSummary? = null,
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val quoteItems: List<QuoteItem> = emptyList(),
    val editingItemIndex: Int = -1,
    val editingQty: String = "",
    val editingPrice: String = "",
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int = 1,
    val isSubmitting: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    val resultQuoteNumber: String? = null,
    val resultTotal: Double = 0.0,
    val showDone: Boolean = false
)

/* ─── ViewModel ─── */

class CreateQuoteViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreateQuoteUiState())
    val state: StateFlow<CreateQuoteUiState> = _state.asStateFlow()

    fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getWarehouses()
                val whs = resp.body()?.data ?: emptyList()
                _state.value = _state.value.copy(
                    warehouses = whs, selectedWarehouseId = whs.firstOrNull()?.id ?: 1
                )
            } catch (_: Exception) {}
        }
    }

    fun searchCustomers(query: String) {
        _state.value = _state.value.copy(customerSearch = query)
        if (query.length < 2) { _state.value = _state.value.copy(customerResults = emptyList()); return }
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
            customerSearch = "",
            customerResults = emptyList()
        )
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
            _state.value = s.copy(quoteItems = s.quoteItems.toList(), feedback = "${product.skuCode} x${existing.qty}", feedbackError = false)
        } else {
            _state.value = s.copy(
                quoteItems = s.quoteItems + QuoteItem(product = product),
                feedback = "已加入: ${product.skuCode}", feedbackError = false,
                searchResults = emptyList(), searchQuery = ""
            )
        }
    }

    fun scanBarcode(code: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(code)
                val products = resp.body()?.data ?: emptyList()
                if (products.size == 1) addToQuote(products.first())
                else if (products.isNotEmpty()) _state.value = _state.value.copy(searchResults = products, searchQuery = code)
                else _state.value = _state.value.copy(feedback = "找不到: $code", feedbackError = true)
            } catch (_: Exception) {}
        }
    }

    fun startEditItem(index: Int) {
        val item = _state.value.quoteItems.getOrNull(index) ?: return
        _state.value = _state.value.copy(editingItemIndex = index, editingQty = item.qty.toString(), editingPrice = if (item.unitPrice > 0) item.unitPrice.toString() else "")
    }

    fun cancelEditItem() { _state.value = _state.value.copy(editingItemIndex = -1) }
    fun updateEditingQty(v: String) { _state.value = _state.value.copy(editingQty = v) }
    fun updateEditingPrice(v: String) { _state.value = _state.value.copy(editingPrice = v) }

    fun saveEditItem() {
        val s = _state.value; val idx = s.editingItemIndex
        if (idx < 0) return; val item = s.quoteItems.getOrNull(idx) ?: return
        item.qty = s.editingQty.toIntOrNull()?.coerceAtLeast(1) ?: item.qty
        item.unitPrice = s.editingPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: item.unitPrice
        _state.value = s.copy(quoteItems = s.quoteItems.toList(), editingItemIndex = -1)
    }

    fun adjustQty(index: Int, delta: Int) {
        val item = _state.value.quoteItems.getOrNull(index) ?: return
        item.qty = (item.qty + delta).coerceAtLeast(1)
        _state.value = _state.value.copy(quoteItems = _state.value.quoteItems.toList())
    }

    fun removeQuoteItem(index: Int) {
        _state.value = _state.value.copy(
            quoteItems = _state.value.quoteItems.toMutableList().also { it.removeAt(index) },
            editingItemIndex = -1
        )
    }

    fun selectWarehouse(id: Int) { _state.value = _state.value.copy(selectedWarehouseId = id) }

    fun submitQuotation() {
        val s = _state.value; val cust = s.selectedCustomer ?: return
        if (s.quoteItems.isEmpty()) { _state.value = s.copy(feedback = "請先加入項目", feedbackError = true); return }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val req = CreateQuotationRequest(customerId = cust.id, warehouseId = s.selectedWarehouseId,
                    items = s.quoteItems.map { QuoteItemRequest(productId = it.product.id, qty = it.qty, unitPrice = it.unitPrice) })
                val resp = ApiClient.service.createQuotation(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val d = resp.body()?.data
                    _state.value = s.copy(isSubmitting = false, showDone = true, resultQuoteNumber = d?.invoiceNumber ?: "", resultTotal = d?.grandTotalHkd ?: 0.0)
                } else {
                    _state.value = s.copy(isSubmitting = false, feedback = resp.body()?.error?.message ?: "建立失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = s.copy(isSubmitting = false, feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
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
   SCREEN — ALL INLINE, no dialogs
   ═══════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(
    scannerManager: ScannerManager,
    viewModel: CreateQuoteViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWarehouses()
        scannerManager.scanResults.collect { result ->
            viewModel.scanBarcode(result.code)
            ScanFeedback.success()
        }
    }

    if (state.showDone) {
        DoneScreen(state, viewModel)
        return
    }

    val grandTotal = state.quoteItems.sumOf { it.lineTotal }
    val totalItems = state.quoteItems.sumOf { it.qty }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("報價單") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TileOrange, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            if (state.quoteItems.isNotEmpty()) {
                BottomBar(state, viewModel, grandTotal, totalItems)
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // Feedback
            state.feedback?.let { msg ->
                item {
                    Surface(Modifier.fillMaxWidth(), color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                        Text(msg, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ═══ 顧客 ═══
            item { CustomerSection(state, viewModel) }

            // ═══ 項目 ═══
            item { ItemsSectionHeader(state, viewModel) }
            if (state.searchResults.isNotEmpty()) item { SearchResultsCard(state, viewModel) }
            items(state.quoteItems.size) { index -> QuoteItemRow(index, state, viewModel) }
            if (state.quoteItems.isEmpty() && !state.isSubmitting) item { EmptyHint() }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ═══ 顧客 ═══ */

@Composable
fun CustomerSection(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("顧客", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileOrange)
        }
        Spacer(Modifier.height(8.dp))

        if (state.selectedCustomer != null) {
            // ── Selected customer ──
            val c = state.selectedCustomer!!
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(c.companyNameZh.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = TileOrange)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold)
                        c.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                    }
                    if (c.hasOutstanding) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Error.copy(alpha = 0.1f)) {
                            Text("未付 HKD ${c.outstandingDisplay}", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Error)
                        }
                    }
                }
            }
        } else {
            // ── Search + results inline ──
            OutlinedTextField(
                value = state.customerSearch, onValueChange = vm::searchCustomers,
                placeholder = { Text("選擇客戶") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TileOrange, cursorColor = TileOrange)
            )

            // Inline results
            if (state.customerResults.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                    Column {
                        state.customerResults.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.selectCustomer(c) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text(c.companyNameZh.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = TileOrange, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold)
                                    c.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                }
                            }
                            if (c != state.customerResults.last()) HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            } else if (state.customerSearch.length >= 2) {
                Spacer(Modifier.height(6.dp))
                Text("無匹配客戶", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(4.dp))
            }
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}

/* ═══ 項目 Header ═══ */

@Composable
fun ItemsSectionHeader(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("項目", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TileOrange)
            Spacer(Modifier.weight(1f))
            Text("+ 新增項目", color = TileOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.searchQuery, onValueChange = vm::searchProducts,
            placeholder = { Text("掃描條碼 或 搜尋 SKU / 名稱...") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TileOrange, cursorColor = TileOrange)
        )
    }
}

/* ═══ Search Results ═══ */

@Composable
fun SearchResultsCard(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.2f))
    ) {
        Column {
            Text("搜尋結果 (${state.searchResults.size})", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            state.searchResults.take(5).forEach { p ->
                Row(Modifier.fillMaxWidth().clickable { vm.addToQuote(p) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inventory2, null, tint = TileOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.skuCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(p.nameZh, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Icon(Icons.Filled.AddCircle, "加入", tint = Success, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/* ═══ Quote Item Row ═══ */

@Composable
fun QuoteItemRow(index: Int, state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    val item = state.quoteItems[index]
    val isEditing = state.editingItemIndex == index
    val price = if (item.unitPrice > 0) item.unitPrice else item.product.retailPriceHkd

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
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
                Row {
                    OutlinedTextField(value = state.editingQty, onValueChange = vm::updateEditingQty, label = { Text("數量") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = state.editingPrice, onValueChange = vm::updateEditingPrice, label = { Text("單價") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = vm::cancelEditItem) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = vm::saveEditItem, colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(10.dp)) { Text("儲存") }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.adjustQty(index, -1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, "減", Modifier.size(14.dp)) }
                            Text("${item.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                            IconButton(onClick = { vm.adjustQty(index, 1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "加", Modifier.size(14.dp)) }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("小計 HKD ${"%,.2f".format(item.lineTotal)}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.startEditItem(index) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.Edit, "編輯", tint = TileOrange, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { vm.removeQuoteItem(index) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.DeleteOutline, "刪除", tint = Error, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

/* ═══ Empty Hint ═══ */

@Composable
fun EmptyHint() {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))) {
        Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AddShoppingCart, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("掃描條碼或搜尋商品加入報價", color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

/* ═══ Bottom Bar ═══ */

@Composable
fun BottomBar(state: CreateQuoteUiState, vm: CreateQuoteViewModel, grandTotal: Double, totalItems: Int) {
    Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$totalItems 項", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("HKD ${"%,.2f".format(grandTotal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TileOrange)
            }
            Button(
                onClick = vm::submitQuotation,
                enabled = state.selectedCustomer != null && state.quoteItems.isNotEmpty() && !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)
            ) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("提交報價", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ═══ Done ═══ */

@Composable
fun DoneScreen(state: CreateQuoteUiState, vm: CreateQuoteViewModel) {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Success)
            Spacer(Modifier.height(20.dp))
            Text("報價建立完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.05f)), border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))) {
                Column(Modifier.padding(20.dp)) {
                    Text("報價單號", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(state.resultQuoteNumber ?: "--", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = TileOrange)
                    Spacer(Modifier.height(8.dp))
                    Text("金額: HKD ${"%,.2f".format(state.resultTotal)}", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = vm::newQuotation, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(12.dp)) {
                Text("建立新報價", fontWeight = FontWeight.Bold)
            }
        }
    }
}
