package com.erp.pda.ui.createquote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuoteItem(
    val product: Product,
    var qty: Int = 1,
    var unitPrice: Double = 0.0
) {
    val lineTotal: Double get() = qty * unitPrice
}

enum class QuoteStep { CUSTOMER, ITEMS, REVIEW, DONE }

data class CreateQuoteUiState(
    val step: QuoteStep = QuoteStep.CUSTOMER,
    // Customer step
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerSummary? = null,
    // Items step
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val quoteItems: List<QuoteItem> = emptyList(),
    val editingItemIndex: Int = -1, // -1 means not editing
    val editingQty: String = "",
    val editingPrice: String = "",
    // Warehouse
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int = 1,
    // Status
    val isLoading: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    // Result
    val resultQuoteNumber: String? = null,
    val resultTotal: Double = 0.0
)

class CreateQuoteViewModel : ViewModel() {
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

    // ─── Step 1: Customer ───

    fun searchCustomers(query: String) {
        _state.value = _state.value.copy(customerSearch = query)
        if (query.length < 2) {
            _state.value = _state.value.copy(customerResults = emptyList())
            return
        }
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchCustomers(query)
                _state.value = _state.value.copy(
                    customerResults = resp.body()?.data ?: emptyList()
                )
            } catch (_: Exception) {}
        }
    }

    fun selectCustomer(c: CustomerSummary) {
        _state.value = _state.value.copy(selectedCustomer = c)
    }

    fun goToItems() {
        if (_state.value.selectedCustomer == null) {
            _state.value = _state.value.copy(
                feedback = "請選擇客戶", feedbackError = true
            )
            return
        }
        _state.value = _state.value.copy(step = QuoteStep.ITEMS)
    }

    fun backFromItems() {
        _state.value = _state.value.copy(step = QuoteStep.CUSTOMER)
    }

    // ─── Step 2: Quote Items ───

    fun searchProducts(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.length < 2) return
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(query)
                _state.value = _state.value.copy(
                    searchResults = resp.body()?.data ?: emptyList()
                )
            } catch (_: Exception) {}
        }
    }

    fun addToQuote(product: Product) {
        val s = _state.value
        val existing = s.quoteItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.qty++
            _state.value = s.copy(
                feedback = "${product.skuCode} x${existing.qty}", feedbackError = false
            )
        } else {
            val newItems = s.quoteItems + QuoteItem(product = product)
            _state.value = s.copy(
                quoteItems = newItems,
                feedback = "已加入: ${product.skuCode}", feedbackError = false
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
                    _state.value = _state.value.copy(
                        searchResults = products, searchQuery = code
                    )
                } else {
                    _state.value = _state.value.copy(
                        feedback = "找不到商品: $code", feedbackError = true
                    )
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

    fun cancelEditItem() {
        _state.value = _state.value.copy(editingItemIndex = -1)
    }

    fun updateEditingQty(value: String) {
        _state.value = _state.value.copy(editingQty = value)
    }

    fun updateEditingPrice(value: String) {
        _state.value = _state.value.copy(editingPrice = value)
    }

    fun saveEditItem() {
        val s = _state.value
        val idx = s.editingItemIndex
        if (idx < 0) return
        val item = s.quoteItems.getOrNull(idx) ?: return
        item.qty = s.editingQty.toIntOrNull()?.coerceAtLeast(1) ?: item.qty
        item.unitPrice = s.editingPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: item.unitPrice
        _state.value = s.copy(
            quoteItems = s.quoteItems.toList(),
            editingItemIndex = -1,
            editingQty = "",
            editingPrice = ""
        )
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

    fun goToReview() {
        if (_state.value.quoteItems.isEmpty()) {
            _state.value = _state.value.copy(
                feedback = "請先加入報價項目", feedbackError = true
            )
            return
        }
        _state.value = _state.value.copy(step = QuoteStep.REVIEW)
    }

    fun backFromReview() {
        _state.value = _state.value.copy(step = QuoteStep.ITEMS)
    }

    // ─── Step 3: Review + Submit ───

    fun selectWarehouse(id: Int) {
        _state.value = _state.value.copy(selectedWarehouseId = id)
    }

    fun submitQuotation() {
        val s = _state.value
        val cust = s.selectedCustomer ?: return
        if (s.selectedWarehouseId <= 0) {
            _state.value = s.copy(
                feedback = "請選擇倉庫", feedbackError = true
            )
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            try {
                val items = s.quoteItems.map { qi ->
                    QuoteItemRequest(
                        productId = qi.product.id,
                        qty = qi.qty,
                        unitPrice = qi.unitPrice
                    )
                }
                val req = CreateQuotationRequest(
                    customerId = cust.id,
                    warehouseId = s.selectedWarehouseId,
                    items = items
                )
                val resp = ApiClient.service.createQuotation(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val data = resp.body()?.data
                    _state.value = s.copy(
                        isLoading = false,
                        step = QuoteStep.DONE,
                        resultQuoteNumber = data?.invoiceNumber ?: "",
                        resultTotal = data?.grandTotalHkd ?: 0.0
                    )
                } else {
                    _state.value = s.copy(
                        isLoading = false,
                        feedback = resp.body()?.error?.message ?: "建立報價失敗",
                        feedbackError = true
                    )
                }
            } catch (e: Exception) {
                _state.value = s.copy(
                    isLoading = false,
                    feedback = "網絡錯誤: ${e.localizedMessage}",
                    feedbackError = true
                )
            }
        }
    }

    fun newQuotation() {
        val whs = _state.value.warehouses
        _state.value = CreateQuoteUiState(
            warehouses = whs,
            selectedWarehouseId = whs.firstOrNull()?.id ?: 1
        )
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(feedback = null)
    }
}
