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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
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

/* ─── 思考：PDA 報價單最實用嘅 workflow ───
   1. 一開 page 就 ready scan → 掃 barcode 直接加項目
   2. 揀客 → click 搜尋 / 掃客戶卡
   3. 睇住個 total 一路 build up
   4. 撳一下就提交 → 完成
   5. 最少打字、最少 click
   6. 每個動作有 feedback（聲 + toast）
*/

data class QuoteItem(var product: Product, var qty: Int = 1, var unitPrice: Double = 0.0, var customName: String? = null) {
    val lineTotal get() = qty * unitPrice
    val displayPrice get() = if (unitPrice > 0) unitPrice else product.retailPriceHkd
    val displayName get() = customName ?: product.nameZh.ifBlank { product.skuCode }
    val isCustom get() = customName != null
}

data class QuoteState(
    val customer: CustomerSummary? = null,
    val items: List<QuoteItem> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val warehouseId: Int = 1,
    val submitting: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    // Done
    val done: Boolean = false,
    val resultNumber: String = "",
    val resultTotal: Double = 0.0,
    // Picker
    val pickingCustomer: Boolean = false,
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    // Create customer inline
    val creatingCustomer: Boolean = false,
    val newCustName: String = "",
    val newCustPhone: String = "",
    // Add item
    val pickingProduct: Boolean = false,
    val productSearch: String = "",
    val productResults: List<Product> = emptyList(),
    // Create custom item (no product match)
    val creatingCustomItem: Boolean = false,
    val customItemName: String = "",
    val customItemPrice: String = "",
    // Edit item (inline)
    val editIdx: Int = -1,
    val editQty: String = "",
    val editPrice: String = ""
) {
    val total get() = items.sumOf { it.lineTotal }
    val itemCount get() = items.sumOf { it.qty }
}

class CreateQuoteViewModel : ViewModel() {
    private val _s = MutableStateFlow(QuoteState())
    val s: StateFlow<QuoteState> = _s.asStateFlow()

    init { loadWarehouses() }

    fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val whs = ApiClient.service.getWarehouses().body()?.data ?: emptyList()
                _s.value = _s.value.copy(warehouses = whs, warehouseId = whs.firstOrNull()?.id ?: 1)
            } catch (_: Exception) {}
        }
    }

    // ── Customer ──
    fun openPickCustomer() {
        _s.value = _s.value.copy(pickingCustomer = true, customerSearch = "", customerResults = emptyList())
        // 開咗即刻 load 全部客戶
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getAllCustomers()
                _s.value = _s.value.copy(customerResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }
    fun closePickCustomer() { _s.value = _s.value.copy(pickingCustomer = false) }
    fun searchCustomer(q: String) {
        _s.value = _s.value.copy(customerSearch = q)
        if (q.length < 2) { _s.value = _s.value.copy(customerResults = emptyList()); return }
        viewModelScope.launch {
            try { _s.value = _s.value.copy(customerResults = ApiClient.service.searchCustomers(q).body()?.data ?: emptyList()) }
            catch (_: Exception) {}
        }
    }
    fun selectCustomer(c: CustomerSummary) { _s.value = _s.value.copy(customer = c, pickingCustomer = false) }

    // ── Create new customer inline ──
    fun startCreateCustomer() { _s.value = _s.value.copy(creatingCustomer = true, newCustName = "", newCustPhone = "") }
    fun cancelCreateCustomer() { _s.value = _s.value.copy(creatingCustomer = false) }
    fun setNewCustName(v: String) { _s.value = _s.value.copy(newCustName = v) }
    fun setNewCustPhone(v: String) { _s.value = _s.value.copy(newCustPhone = v) }
    fun submitNewCustomer() {
        val name = _s.value.newCustName.trim()
        if (name.isBlank()) { _s.value = _s.value.copy(feedback = "請輸入客戶名稱", feedbackError = true); return }
        viewModelScope.launch {
            try {
                val req = CreateCustomerRequest(companyNameZh = name, contactPhone = _s.value.newCustPhone.ifBlank { null })
                val resp = ApiClient.service.createCustomer(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    resp.body()?.data?.let { _s.value = _s.value.copy(customer = it, pickingCustomer = false, creatingCustomer = false) }
                } else _s.value = _s.value.copy(feedback = resp.body()?.error?.message ?: "建立失敗", feedbackError = true)
            } catch (e: Exception) { _s.value = _s.value.copy(feedback = "錯誤: ${e.localizedMessage}", feedbackError = true) }
        }
    }

    // ── Products ──
    fun openPickProduct() {
        _s.value = _s.value.copy(pickingProduct = true, productSearch = "", productResults = emptyList())
        // 開咗即刻 load 全部商品
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts("")  // empty query returns all
                _s.value = _s.value.copy(productResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }
    fun closePickProduct() { _s.value = _s.value.copy(pickingProduct = false) }
    fun searchProduct(q: String) {
        _s.value = _s.value.copy(productSearch = q)
        if (q.length < 2) return
        viewModelScope.launch {
            try { _s.value = _s.value.copy(productResults = ApiClient.service.searchProducts(q).body()?.data ?: emptyList()) }
            catch (_: Exception) {}
        }
    }
    fun addProduct(p: Product) {
        val st = _s.value
        val ex = st.items.find { it.product.id == p.id }
        if (ex != null) { ex.qty++; _s.value = st.copy(items = st.items.toList(), feedback = "${p.skuCode} ×${ex.qty}") }
        else _s.value = st.copy(items = st.items + QuoteItem(p), pickingProduct = false, feedback = "已加入 ${p.skuCode}")
    }

    // ── Custom item (no product match) ──
    fun startCustomItem() {
        _s.value = _s.value.copy(creatingCustomItem = true, customItemName = _s.value.productSearch, customItemPrice = "")
    }
    fun cancelCustomItem() { _s.value = _s.value.copy(creatingCustomItem = false) }
    fun setCustomName(v: String) { _s.value = _s.value.copy(customItemName = v) }
    fun setCustomPrice(v: String) { _s.value = _s.value.copy(customItemPrice = v) }
    fun submitCustomItem() {
        val name = _s.value.customItemName.trim()
        if (name.isBlank()) { _s.value = _s.value.copy(feedback = "請輸入名稱", feedbackError = true); return }
        val price = _s.value.customItemPrice.toDoubleOrNull() ?: 0.0
        val dummyProduct = Product(id = 0, skuCode = "CUSTOM", nameZh = name)
        val item = QuoteItem(product = dummyProduct, unitPrice = price, customName = name)
        _s.value = _s.value.copy(
            items = _s.value.items + item,
            pickingProduct = false, creatingCustomItem = false,
            feedback = "已加入: $name"
        )
    }

    // ── Barcode scan ──
    fun onScan(code: String) {
        viewModelScope.launch {
            try {
                val prods = ApiClient.service.searchProducts(code).body()?.data ?: emptyList()
                when {
                    prods.size == 1 -> addProduct(prods.first())
                    prods.isNotEmpty() -> _s.value = _s.value.copy(pickingProduct = true, productResults = prods, productSearch = code)
                    else -> _s.value = _s.value.copy(feedback = "找不到: $code", feedbackError = true)
                }
            } catch (_: Exception) {}
        }
    }

    // ── Edit ──
    fun startEdit(idx: Int) {
        val it = _s.value.items.getOrNull(idx) ?: return
        _s.value = _s.value.copy(editIdx = idx, editQty = it.qty.toString(), editPrice = if (it.unitPrice > 0) it.unitPrice.toString() else "")
    }
    fun cancelEdit() { _s.value = _s.value.copy(editIdx = -1) }
    fun updateEditQty(v: String) { _s.value = _s.value.copy(editQty = v) }
    fun updateEditPrice(v: String) { _s.value = _s.value.copy(editPrice = v) }
    fun saveEdit() {
        val st = _s.value; val idx = st.editIdx; if (idx < 0) return
        val it = st.items[idx]
        it.qty = st.editQty.toIntOrNull()?.coerceAtLeast(1) ?: it.qty
        it.unitPrice = st.editPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: it.unitPrice
        _s.value = st.copy(items = st.items.toList(), editIdx = -1)
    }
    fun adjQty(idx: Int, d: Int) {
        val it = _s.value.items.getOrNull(idx) ?: return
        it.qty = (it.qty + d).coerceAtLeast(1)
        _s.value = _s.value.copy(items = _s.value.items.toList())
    }
    fun removeItem(idx: Int) {
        _s.value = _s.value.copy(items = _s.value.items.toMutableList().also { it.removeAt(idx) })
    }

    // ── Submit ──
    fun submit() {
        val st = _s.value; val cust = st.customer ?: return
        if (st.items.isEmpty()) { _s.value = st.copy(feedback = "請先加入項目", feedbackError = true); return }
        viewModelScope.launch {
            _s.value = st.copy(submitting = true)
            try {
                val req = CreateQuotationRequest(customerId = cust.id, warehouseId = st.warehouseId,
                    items = st.items.map { QuoteItemRequest(productId = if (it.isCustom) 0 else it.product.id, productName = it.customName, qty = it.qty, unitPrice = it.unitPrice) })
                val resp = ApiClient.service.createQuotation(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val d = resp.body()?.data
                    _s.value = st.copy(submitting = false, done = true, resultNumber = d?.invoiceNumber ?: "", resultTotal = d?.grandTotalHkd ?: 0.0)
                } else _s.value = st.copy(submitting = false, feedback = resp.body()?.error?.message ?: "失敗", feedbackError = true)
            } catch (e: Exception) { _s.value = st.copy(submitting = false, feedback = "錯誤: ${e.localizedMessage}", feedbackError = true) }
        }
    }

    fun reset() { _s.value = QuoteState(warehouses = _s.value.warehouses, warehouseId = _s.value.warehouses.firstOrNull()?.id ?: 1) }
    fun clearFeedback() { _s.value = _s.value.copy(feedback = null) }
}

// ═══════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(scannerManager: ScannerManager, viewModel: CreateQuoteViewModel = viewModel()) {
    val s by viewModel.s.collectAsState()

    // Scanner always listening
    LaunchedEffect(Unit) {
        viewModel.loadWarehouses() // re-trigger via init
        scannerManager.scanResults.collect { result ->
            viewModel.onScan(result.code)
            ScanFeedback.success()
        }
    }

    // ── Done ──
    if (s.done) {
        Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(72.dp), tint = Success)
                Spacer(Modifier.height(16.dp))
                Text("報價建立完成！", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(s.resultNumber, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TileOrange)
                Text("HKD ${"%,.2f".format(s.resultTotal)}", fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(32.dp))
                Button(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(12.dp)) {
                    Text("開新報價單", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("報價單", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TileOrange, titleContentColor = Color.White))
        },
        bottomBar = {
            // Always show bottom bar with total + submit
            Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${s.itemCount} 項", fontSize = 11.sp, color = Color.Gray)
                        Text("HKD ${"%,.2f".format(s.total)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TileOrange)
                    }
                    Button(onClick = viewModel::submit,
                        enabled = s.customer != null && s.items.isNotEmpty() && !s.submitting,
                        colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
                        if (s.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("提交", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // ── Feedback toast ──
            s.feedback?.let {
                item {
                    Surface(Modifier.fillMaxWidth(), color = if (s.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.15f)) {
                        Text(it, Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // ═══ ① 顧客 ═══
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("顧客", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TileOrange, modifier = Modifier.width(50.dp))
                    if (s.customer != null) {
                        val c = s.customer!!
                        // Show customer name with edit button
                        Row(Modifier.weight(1f).clickable { viewModel.openPickCustomer() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Text(c.companyNameZh.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = TileOrange, fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        IconButton(onClick = viewModel::openPickCustomer, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Edit, "更換", tint = TileOrange, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        TextButton(onClick = viewModel::openPickCustomer, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Text("選擇客戶 ＋", color = TileOrange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFF0F0F0))
            }

            // ═══ ② 掃描提示 ═══
            item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp), color = TileOrange.copy(alpha = 0.05f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCodeScanner, null, tint = TileOrange.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("掃描條碼加入商品", fontSize = 13.sp, color = TileOrange.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::openPickProduct, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("或搜尋 ＋", fontSize = 13.sp, color = TileOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ═══ ③ 項目列表 ═══
            if (s.items.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddShoppingCart, null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("掃描條碼開始加入商品", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            items(s.items.size) { idx ->
                ItemRow(idx, s, viewModel)
            }

            // ── "Add more" button ──
            if (s.items.isNotEmpty()) {
                item {
                    TextButton(onClick = viewModel::openPickProduct, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Icon(Icons.Filled.Add, null, tint = TileOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("加入更多項目", color = TileOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Customer Picker Dialog ──
    if (s.pickingCustomer && !s.creatingCustomer) {
        AlertDialog(onDismissRequest = viewModel::closePickCustomer,
            title = { Text("選擇客戶", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = s.customerSearch, onValueChange = viewModel::searchCustomer,
                        placeholder = { Text("搜尋客戶...") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.height(8.dp))
                    if (s.customerResults.isEmpty() && s.customerSearch.length >= 2) {
                        Text("無匹配結果", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(s.customerResults) { c ->
                            Row(Modifier.fillMaxWidth().clickable { viewModel.selectCustomer(c) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text(c.companyNameZh.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = TileOrange, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    c.contactPhone?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TextButton(onClick = viewModel::startCreateCustomer, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PersonAdd, null, tint = TileOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("新增客戶", color = TileOrange, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closePickCustomer) { Text("取消", color = Color.Gray) } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp))
    }

    // ── Create Customer Dialog ──
    if (s.creatingCustomer) {
        AlertDialog(onDismissRequest = viewModel::cancelCreateCustomer,
            title = { Text("新增客戶", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = s.newCustName, onValueChange = viewModel::setNewCustName,
                        label = { Text("客戶名稱 *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = s.newCustPhone, onValueChange = viewModel::setNewCustPhone,
                        label = { Text("電話 (可選)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                Button(onClick = viewModel::submitNewCustomer,
                    colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(10.dp)) {
                    Text("建立")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelCreateCustomer) { Text("返回") } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp))
    }

    // ── Product Picker Dialog ──
    if (s.pickingProduct && !s.creatingCustomItem) {
        AlertDialog(onDismissRequest = viewModel::closePickProduct,
            title = { Text("加入商品", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = s.productSearch, onValueChange = viewModel::searchProduct,
                        placeholder = { Text("搜尋 SKU 或掃描條碼...") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.height(8.dp))
                    if (s.productResults.isNotEmpty()) {
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(s.productResults) { p ->
                                Row(Modifier.fillMaxWidth().clickable { viewModel.addProduct(p) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Inventory2, null, tint = TileOrange, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(p.skuCode, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(p.nameZh, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Filled.AddCircle, null, tint = Success, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    } else if (s.productSearch.isNotBlank()) {
                        Text("無匹配商品", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TextButton(onClick = viewModel::startCustomItem, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.NoteAdd, null, tint = TileOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("手動新增項目", color = TileOrange, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::closePickProduct) { Text("取消", color = Color.Gray) } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp))
    }

    // ── Custom Item Dialog ──
    if (s.creatingCustomItem) {
        AlertDialog(onDismissRequest = viewModel::cancelCustomItem,
            title = { Text("手動新增項目", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = s.customItemName, onValueChange = viewModel::setCustomName,
                        label = { Text("名稱 *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = s.customItemPrice, onValueChange = viewModel::setCustomPrice,
                        label = { Text("單價 HKD (可選)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                Button(onClick = viewModel::submitCustomItem,
                    colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(10.dp)) {
                    Text("加入")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelCustomItem) { Text("返回") } },
            containerColor = Color.White, shape = RoundedCornerShape(16.dp))
    }
}

// ─── Item Row ───

@Composable
private fun ItemRow(idx: Int, s: QuoteState, vm: CreateQuoteViewModel) {
    val it = s.items[idx]
    val editing = s.editIdx == idx
    val price = it.displayPrice

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // ── Top row: number + name + price ──
        Row(Modifier.fillMaxWidth().clickable { if (!editing) vm.startEdit(idx) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(TileOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TileOrange)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(it.product.nameZh.ifBlank { it.product.skuCode }, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(it.product.skuCode, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.width(8.dp))
            Text("HK$${"%,.2f".format(price)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
        }

        // ── Edit mode ──
        if (editing) {
            Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = s.editQty, onValueChange = { vm.updateEditQty(it) }, label = { Text("數量") },
                    singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = s.editPrice, onValueChange = { vm.updateEditPrice(it) }, label = { Text("單價") },
                    singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = vm::cancelEdit) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = vm::saveEdit, colors = ButtonDefaults.buttonColors(containerColor = TileOrange), shape = RoundedCornerShape(8.dp)) { Text("確定") }
            }
        } else {
            // ── Display mode: qty controls ──
            Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.adjQty(idx, -1) }, modifier = Modifier.size(32.dp)) {
                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                        }
                        Text("${it.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 14.sp)
                        IconButton(onClick = { vm.adjQty(idx, 1) }, modifier = Modifier.size(32.dp)) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("小計 HK$${"%,.2f".format(it.lineTotal)}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.removeItem(idx) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "刪除", tint = Color(0xFFCCCCCC), modifier = Modifier.size(16.dp))
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF5F5F5))
    }
}
