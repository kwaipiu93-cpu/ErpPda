package com.erp.pda.ui.createpo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PoLineItem(
    val product: Product,
    var qty: Int = 1,
    var unitPrice: Double = 0.0
) {
    val lineTotal: Double get() = qty * unitPrice
}

data class CreatePoUiState(
    // Supplier
    val supplierSearch: String = "",
    val suppliers: List<SupplierSummary> = emptyList(),
    val selectedSupplier: SupplierSummary? = null,
    // Products
    val productSearch: String = "",
    val productResults: List<Product> = emptyList(),
    val quoteItems: List<PoLineItem> = emptyList(),
    val editIndex: Int = -1,
    val editQty: String = "",
    val editPrice: String = "",
    // Warehouse
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int = 1,
    // Notes
    val notes: String = "",
    // Status
    val isLoading: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    // Result
    val resultPoNumber: String? = null,
    val step: Int = 0 // 0=select supplier, 1=add items, 2=done
)

class CreatePoViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreatePoUiState())
    val state: StateFlow<CreatePoUiState> = _state.asStateFlow()

    fun loadSuppliersAndWarehouses() {
        viewModelScope.launch {
            try {
                val supResp = ApiClient.service.getSuppliers()
                _state.value = _state.value.copy(suppliers = supResp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
            try {
                val whResp = ApiClient.service.getWarehouses()
                _state.value = _state.value.copy(warehouses = whResp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    fun searchSuppliers(q: String) { _state.value = _state.value.copy(supplierSearch = q) }
    fun selectSupplier(s: SupplierSummary) { _state.value = _state.value.copy(selectedSupplier = s) }
    fun goToItems() {
        if (_state.value.selectedSupplier == null) {
            _state.value = _state.value.copy(feedback = "請選擇供應商", feedbackError = true)
            return
        }
        _state.value = _state.value.copy(step = 1)
    }

    fun searchProducts(q: String) {
        _state.value = _state.value.copy(productSearch = q)
        if (q.length < 2) return
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(q)
                _state.value = _state.value.copy(productResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    fun addToPo(product: Product) {
        val s = _state.value
        val existing = s.quoteItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.qty++
            _state.value = s.copy(feedback = "${product.skuCode} x${existing.qty}", feedbackError = false)
        } else {
            _state.value = s.copy(
                quoteItems = s.quoteItems + PoLineItem(product = product),
                feedback = "已加入: ${product.skuCode}", feedbackError = false
            )
        }
    }

    fun scanBarcode(code: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(code)
                val products = resp.body()?.data ?: emptyList()
                if (products.size == 1) addToPo(products.first())
                else _state.value = _state.value.copy(productResults = products, productSearch = code)
            } catch (_: Exception) {}
        }
    }

    fun removeItem(index: Int) {
        val items = _state.value.quoteItems.toMutableList()
        items.removeAt(index)
        _state.value = _state.value.copy(quoteItems = items)
    }

    fun adjustQty(index: Int, delta: Int) {
        val s = _state.value
        val item = s.quoteItems.getOrNull(index) ?: return
        item.qty = (item.qty + delta).coerceAtLeast(1)
        _state.value = s.copy(quoteItems = s.quoteItems.toList())
    }

    fun startEdit(index: Int) {
        val item = _state.value.quoteItems.getOrNull(index) ?: return
        _state.value = _state.value.copy(editIndex = index, editQty = item.qty.toString(), editPrice = item.unitPrice.toString())
    }
    fun updateEditQty(v: String) { _state.value = _state.value.copy(editQty = v) }
    fun updateEditPrice(v: String) { _state.value = _state.value.copy(editPrice = v) }
    fun saveEdit() {
        val s = _state.value
        val item = s.quoteItems.getOrNull(s.editIndex) ?: return
        item.qty = s.editQty.toIntOrNull()?.coerceAtLeast(1) ?: item.qty
        item.unitPrice = s.editPrice.toDoubleOrNull() ?: item.unitPrice
        _state.value = s.copy(editIndex = -1)
    }
    fun cancelEdit() { _state.value = _state.value.copy(editIndex = -1) }

    fun selectWarehouse(id: Int) { _state.value = _state.value.copy(selectedWarehouseId = id) }
    fun updateNotes(n: String) { _state.value = _state.value.copy(notes = n) }

    fun submitPo() {
        val s = _state.value
        val sup = s.selectedSupplier ?: return
        if (s.quoteItems.isEmpty()) {
            _state.value = s.copy(feedback = "請加入商品", feedbackError = true)
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            try {
                val items = s.quoteItems.map { it -> PoItemRequest(productId = it.product.id, qtyOrdered = it.qty, unitPriceForeign = it.unitPrice) }
                val req = CreatePoRequest(supplierId = sup.id, warehouseId = s.selectedWarehouseId, items = items, notes = s.notes.ifBlank { null })
                val resp = ApiClient.service.createPurchaseOrder(req)
                if (resp.isSuccessful) {
                    _state.value = s.copy(isLoading = false, step = 2, feedback = null)
                } else {
                    _state.value = s.copy(isLoading = false, feedback = resp.body()?.error?.message ?: "建立失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = s.copy(isLoading = false, feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun newPo() {
        _state.value = CreatePoUiState(suppliers = _state.value.suppliers, warehouses = _state.value.warehouses, selectedWarehouseId = _state.value.selectedWarehouseId)
    }

    fun backToSupplier() { _state.value = _state.value.copy(step = 0) }
    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}
