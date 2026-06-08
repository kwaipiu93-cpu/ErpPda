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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ─── Data ───

data class QuoteItem(
    val product: Product,
    var qty: Int = 1,
    var unitPrice: Double = 0.0
) { val lineTotal get() = qty * unitPrice }

data class CreateQuoteUiState(
    val selectedCustomer: CustomerSummary? = null,
    val quoteItems: List<QuoteItem> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int = 1,
    val isSubmitting: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    val resultQuoteNumber: String? = null,
    val resultTotal: Double = 0.0,
    val showDone: Boolean = false,
    // Picker states
    val showCustomerPicker: Boolean = false,
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    // Item edit
    val editingIndex: Int = -1,
    val editingQty: String = "",
    val editingPrice: String = "",
    // Product search
    val productSearch: String = "",
    val productResults: List<Product> = emptyList(),
    val showProductPicker: Boolean = false
)

// ─── ViewModel ───

class CreateQuoteViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreateQuoteUiState())
    val state: StateFlow<CreateQuoteUiState> = _state.asStateFlow()

    fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val whs = ApiClient.service.getWarehouses().body()?.data ?: emptyList()
                _state.value = _state.value.copy(warehouses = whs, selectedWarehouseId = whs.firstOrNull()?.id ?: 1)
            } catch (_: Exception) {}
        }
    }

    fun openCustomerPicker() { _state.value = _state.value.copy(showCustomerPicker = true, customerSearch = "", customerResults = emptyList()) }
    fun closeCustomerPicker() { _state.value = _state.value.copy(showCustomerPicker = false) }
    fun searchCustomers(q: String) {
        _state.value = _state.value.copy(customerSearch = q)
        if (q.length < 2) { _state.value = _state.value.copy(customerResults = emptyList()); return }
        viewModelScope.launch {
            try { _state.value = _state.value.copy(customerResults = ApiClient.service.searchCustomers(q).body()?.data ?: emptyList()) }
            catch (_: Exception) {}
        }
    }
    fun selectCustomer(c: CustomerSummary) { _state.value = _state.value.copy(selectedCustomer = c, showCustomerPicker = false) }

    fun openProductPicker() { _state.value = _state.value.copy(showProductPicker = true, productSearch = "", productResults = emptyList()) }
    fun closeProductPicker() { _state.value = _state.value.copy(showProductPicker = false) }
    fun searchProducts(q: String) {
        _state.value = _state.value.copy(productSearch = q)
        if (q.length < 2) return
        viewModelScope.launch {
            try { _state.value = _state.value.copy(productResults = ApiClient.service.searchProducts(q).body()?.data ?: emptyList()) }
            catch (_: Exception) {}
        }
    }
    fun addToQuote(p: Product) {
        val s = _state.value
        val existing = s.quoteItems.find { it.product.id == p.id }
        if (existing != null) { existing.qty++; _state.value = s.copy(quoteItems = s.quoteItems.toList()) }
        else _state.value = s.copy(quoteItems = s.quoteItems + QuoteItem(product = p), showProductPicker = false)
    }

    fun scanBarcode(code: String) {
        viewModelScope.launch {
            try {
                val prods = ApiClient.service.searchProducts(code).body()?.data ?: emptyList()
                if (prods.size == 1) addToQuote(prods.first())
                else if (prods.isNotEmpty()) _state.value = _state.value.copy(showProductPicker = true, productResults = prods, productSearch = code)
                else _state.value = _state.value.copy(feedback = "找不到: $code", feedbackError = true)
            } catch (_: Exception) {}
        }
    }

    fun startEdit(idx: Int) {
        val it = _state.value.quoteItems.getOrNull(idx) ?: return
        _state.value = _state.value.copy(editingIndex = idx, editingQty = it.qty.toString(), editingPrice = if (it.unitPrice > 0) it.unitPrice.toString() else "")
    }
    fun cancelEdit() { _state.value = _state.value.copy(editingIndex = -1) }
    fun saveEdit() {
        val s = _state.value; val idx = s.editingIndex
        if (idx < 0) return; val it = s.quoteItems[idx]
        it.qty = s.editingQty.toIntOrNull()?.coerceAtLeast(1) ?: it.qty
        it.unitPrice = s.editingPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: it.unitPrice
        _state.value = s.copy(quoteItems = s.quoteItems.toList(), editingIndex = -1)
    }
    fun updateEditQty(v: String) { _state.value = _state.value.copy(editingQty = v) }
    fun updateEditPrice(v: String) { _state.value = _state.value.copy(editingPrice = v) }
    fun adjustQty(idx: Int, d: Int) {
        val it = _state.value.quoteItems.getOrNull(idx) ?: return
        it.qty = (it.qty + d).coerceAtLeast(1)
        _state.value = _state.value.copy(quoteItems = _state.value.quoteItems.toList())
    }
    fun removeItem(idx: Int) { _state.value = _state.value.copy(quoteItems = _state.value.quoteItems.toMutableList().also { it.removeAt(idx) }) }

    fun submit() {
        val s = _state.value; val cust = s.selectedCustomer ?: return
        if (s.quoteItems.isEmpty()) { _state.value = s.copy(feedback = "請加入項目", feedbackError = true); return }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val req = CreateQuotationRequest(customerId = cust.id, warehouseId = s.selectedWarehouseId,
                    items = s.quoteItems.map { QuoteItemRequest(productId = it.product.id, qty = it.qty, unitPrice = it.unitPrice) })
                val resp = ApiClient.service.createQuotation(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val d = resp.body()?.data
                    _state.value = s.copy(isSubmitting = false, showDone = true, resultQuoteNumber = d?.invoiceNumber ?: "", resultTotal = d?.grandTotalHkd ?: 0.0)
                } else _state.value = s.copy(isSubmitting = false, feedback = resp.body()?.error?.message ?: "失敗", feedbackError = true)
            } catch (e: Exception) { _state.value = s.copy(isSubmitting = false, feedback = "錯誤: ${e.localizedMessage}", feedbackError = true) }
        }
    }

    fun newQuote() { _state.value = CreateQuoteUiState(warehouses = _state.value.warehouses, selectedWarehouseId = _state.value.warehouses.firstOrNull()?.id ?: 1) }
    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}

// ═══════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(scannerManager: ScannerManager, viewModel: CreateQuoteViewModel = viewModel()) {
    val s by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWarehouses()
        scannerManager.scanResults.collect { r -> viewModel.scanBarcode(r.code); ScanFeedback.success() }
    }

    if (s.showDone) {
        Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(80.dp), tint = Success)
                Spacer(Modifier.height(20.dp))
                Text("報價建立完成！", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(12.dp))
                Text(s.resultQuoteNumber ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TileOrange)
                Text("HKD ${"%,.2f".format(s.resultTotal)}", fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(32.dp))
                Button(onClick = viewModel::newQuote, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(12.dp)) {
                    Text("建立新報價", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val total = s.quoteItems.sumOf { it.lineTotal }
    val itemCount = s.quoteItems.sumOf { it.qty }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("報價單", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TileOrange, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            if (s.quoteItems.isNotEmpty() || s.selectedCustomer != null) {
                Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${itemCount} 項", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("HKD ${"%,.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TileOrange)
                        }
                        Button(onClick = viewModel::submit, enabled = s.selectedCustomer != null && s.quoteItems.isNotEmpty() && !s.isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
                            if (s.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("提交報價", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // Feedback
            s.feedback?.let {
                item {
                    Surface(Modifier.fillMaxWidth(), color = if (s.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                        Text(it, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ═══ 顧客 ═══
            item {
                Column(Modifier.padding(16.dp)) {
                    Text("顧客", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TileOrange)
                    Spacer(Modifier.height(8.dp))

                    if (s.selectedCustomer != null) {
                        val c = s.selectedCustomer!!
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = TileOrange.copy(alpha = 0.06f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TileOrange.copy(alpha = 0.2f))) {
                            Row(Modifier.padding(12.dp).clickable { viewModel.openCustomerPicker() }, verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Text(c.companyNameZh.take(1).ifBlank { c.companyNameEn.take(1) }, fontWeight = FontWeight.Bold, color = TileOrange, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    c.contactPhone?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
                            }
                        }
                    } else {
                        TextButton(onClick = viewModel::openCustomerPicker, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Text("選擇客戶", color = TileOrange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFF0F0F0))
            }

            // ═══ 項目 ═══
            item {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("項目", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TileOrange)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::openProductPicker) {
                        Text("+ 新增項目", color = TileOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            items(s.quoteItems.size) { idx ->
                val it = s.quoteItems[idx]
                val price = if (it.unitPrice > 0) it.unitPrice else it.product.retailPriceHkd
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { viewModel.startEdit(idx) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TileOrange)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it.product.nameZh.ifBlank { it.product.skuCode }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(it.product.skuCode, fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("HK$${"%,.2f".format(price)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
                    }
                    // Qty row
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.adjustQty(idx, -1) }, modifier = Modifier.size(30.dp)) { Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                Text("${it.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { viewModel.adjustQty(idx, 1) }, modifier = Modifier.size(30.dp)) { Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("小計 HK$${"%,.2f".format(it.lineTotal)}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeItem(idx) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Close, "刪除", tint = Color(0xFFCCCCCC), modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }

            if (s.quoteItems.isEmpty() && !s.isSubmitting) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("掃描條碼或搜尋商品加入報價", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // ── Customer Picker Bottom Sheet ──
    if (s.showCustomerPicker) {
        AlertDialog(
            onDismissRequest = viewModel::closeCustomerPicker,
            title = { Text("選擇客戶", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = s.customerSearch, onValueChange = viewModel::searchCustomers,
                        placeholder = { Text("搜尋客戶...") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (s.customerResults.isEmpty() && s.customerSearch.length >= 2) {
                        Text("無匹配結果", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(s.customerResults) { c ->
                            Row(Modifier.fillMaxWidth().clickable { viewModel.selectCustomer(c) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text(c.companyNameZh.take(1).ifBlank { "?" }, fontWeight = FontWeight.Bold, color = TileOrange, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    c.contactPhone?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closeCustomerPicker) { Text("取消", color = Color.Gray) } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Product Picker Bottom Sheet ──
    if (s.showProductPicker) {
        AlertDialog(
            onDismissRequest = viewModel::closeProductPicker,
            title = { Text("新增項目", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = s.productSearch, onValueChange = viewModel::searchProducts,
                        placeholder = { Text("搜尋 SKU 或掃描條碼...") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(s.productResults) { p ->
                            Row(Modifier.fillMaxWidth().clickable { viewModel.addToQuote(p) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Inventory2, null, tint = TileOrange, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.skuCode, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(p.nameZh, fontSize = 13.sp, color = Color.Gray)
                                }
                                Icon(Icons.Filled.AddCircle, null, tint = Success, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closeProductPicker) { Text("取消", color = Color.Gray) } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp)
        )
    }
}
