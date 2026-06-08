package com.erp.pda.ui.quotedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditableItem(
    val id: Int,
    val productId: Int,
    val skuCode: String,
    val productName: String,
    var qty: Int,
    var unitPrice: Double,
    val originalQty: Int,
    val originalUnitPrice: Double,
    var isDeleted: Boolean = false
) {
    val lineTotal get() = qty * unitPrice
    val isModified get() = qty != originalQty || unitPrice != originalUnitPrice || isDeleted
}

data class QuoteDetailUiState(
    val detail: InvoiceDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Edit mode
    val editing: Boolean = false,
    val editItems: List<EditableItem> = emptyList(),
    val editNotes: String = "",
    val editCustomerId: Int? = null,
    val editWarehouseId: Int? = null,
    val editCustomerName: String = "",
    val editWarehouseName: String = "",

    // Pickers
    val pickingCustomer: Boolean = false,
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    val pickingWarehouse: Boolean = false,
    val warehouseResults: List<Warehouse> = emptyList(),

    // Add product
    val pickingProduct: Boolean = false,
    val productSearch: String = "",
    val productResults: List<Product> = emptyList(),
    val creatingCustomItem: Boolean = false,
    val customItemName: String = "",
    val customItemPrice: String = "",

    // Edit item inline
    val editItemIdx: Int = -1,
    val editItemQty: String = "",
    val editItemPrice: String = "",

    // Submit
    val saving: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,

    val done: Boolean = false
) {
    val hasChanges: Boolean get() = editItems.any { it.isModified || it.isDeleted } ||
            editNotes != (detail?.notes ?: "") ||
            editCustomerId != null ||
            editWarehouseId != null
}

class QuoteDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(QuoteDetailUiState())
    val state: StateFlow<QuoteDetailUiState> = _state.asStateFlow()

    fun loadDetail(quoteId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getQuotationDetail(quoteId)
                val detail = resp.body()?.data
                _state.value = _state.value.copy(
                    detail = detail,
                    isLoading = false,
                    editNotes = detail?.notes ?: "",
                    editCustomerName = detail?.customerName ?: ""
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    // ── Edit mode ──
    fun startEditing() {
        val d = _state.value.detail ?: return
        _state.value = _state.value.copy(
            editing = true,
            editItems = d.items.map {
                EditableItem(
                    id = it.id,
                    productId = it.productId,
                    skuCode = it.skuCode,
                    productName = it.productName,
                    qty = it.qty,
                    unitPrice = it.unitPrice,
                    originalQty = it.qty,
                    originalUnitPrice = it.unitPrice
                )
            },
            editNotes = d.notes ?: "",
            editCustomerId = null,
            editWarehouseId = null,
            editCustomerName = d.customerName
        )
    }

    fun cancelEditing() {
        _state.value = _state.value.copy(
            editing = false, editItems = emptyList(), editNotes = "",
            editCustomerId = null, editWarehouseId = null,
            editCustomerName = "", editWarehouseName = ""
        )
    }

    // ── Edit notes ──
    fun setEditNotes(v: String) { _state.value = _state.value.copy(editNotes = v) }

    // ── Customer picker ──
    fun openPickCustomer() {
        _state.value = _state.value.copy(pickingCustomer = true, customerSearch = "", customerResults = emptyList())
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getAllCustomers()
                _state.value = _state.value.copy(customerResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }
    fun closePickCustomer() { _state.value = _state.value.copy(pickingCustomer = false) }
    fun searchCustomer(q: String) {
        _state.value = _state.value.copy(customerSearch = q)
        if (q.length < 2) { _state.value = _state.value.copy(customerResults = emptyList()); return }
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(customerResults = ApiClient.service.searchCustomers(q).body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }
    fun selectCustomer(c: CustomerSummary) {
        _state.value = _state.value.copy(
            editCustomerId = c.id,
            editCustomerName = c.companyNameZh.ifBlank { c.companyNameEn },
            pickingCustomer = false
        )
    }

    // ── Warehouse picker ──
    fun openPickWarehouse() {
        _state.value = _state.value.copy(pickingWarehouse = true)
        viewModelScope.launch {
            try {
                val whs = ApiClient.service.getWarehouses().body()?.data ?: emptyList()
                _state.value = _state.value.copy(warehouseResults = whs)
            } catch (_: Exception) {}
        }
    }
    fun closePickWarehouse() { _state.value = _state.value.copy(pickingWarehouse = false) }
    fun selectWarehouse(w: Warehouse) {
        _state.value = _state.value.copy(editWarehouseId = w.id, editWarehouseName = w.nameZh.ifBlank { w.nameEn }, pickingWarehouse = false)
    }

    // ── Edit item ──
    fun startEditItem(idx: Int) {
        val it = _state.value.editItems.getOrNull(idx) ?: return
        _state.value = _state.value.copy(editItemIdx = idx, editItemQty = it.qty.toString(), editItemPrice = it.unitPrice.toString())
    }
    fun cancelEditItem() { _state.value = _state.value.copy(editItemIdx = -1) }
    fun setEditItemQty(v: String) { _state.value = _state.value.copy(editItemQty = v) }
    fun setEditItemPrice(v: String) { _state.value = _state.value.copy(editItemPrice = v) }
    fun saveEditItem() {
        val st = _state.value
        val idx = st.editItemIdx
        if (idx < 0) return
        val items = st.editItems.toMutableList()
        val it = items[idx]
        items[idx] = it.copy(
            qty = st.editItemQty.toIntOrNull()?.coerceAtLeast(1) ?: it.qty,
            unitPrice = st.editItemPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: it.unitPrice
        )
        _state.value = st.copy(editItems = items, editItemIdx = -1)
    }

    fun adjItemQty(idx: Int, d: Int) {
        val items = _state.value.editItems.toMutableList()
        val it = items.getOrNull(idx) ?: return
        items[idx] = it.copy(qty = (it.qty + d).coerceAtLeast(1))
        _state.value = _state.value.copy(editItems = items)
    }

    fun toggleDeleteItem(idx: Int) {
        val items = _state.value.editItems.toMutableList()
        val it = items.getOrNull(idx) ?: return
        items[idx] = it.copy(isDeleted = !it.isDeleted)
        _state.value = _state.value.copy(editItems = items)
    }

    // ── Add product ──
    fun openPickProduct() {
        _state.value = _state.value.copy(pickingProduct = true, productSearch = "", productResults = emptyList())
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts("")
                _state.value = _state.value.copy(productResults = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }
    fun closePickProduct() { _state.value = _state.value.copy(pickingProduct = false) }
    fun searchProduct(q: String) {
        _state.value = _state.value.copy(productSearch = q)
        if (q.length < 2) return
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(productResults = ApiClient.service.searchProducts(q).body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }
    fun addProduct(p: Product) {
        val st = _state.value
        val newItem = EditableItem(
            id = 0, // new item
            productId = p.id,
            skuCode = p.skuCode,
            productName = p.nameZh.ifBlank { p.skuCode },
            qty = 1,
            unitPrice = p.retailPriceHkd,
            originalQty = 0,
            originalUnitPrice = 0.0
        )
        _state.value = st.copy(editItems = st.editItems + newItem, pickingProduct = false, feedback = "已加入 ${p.skuCode}")
    }

    // ── Custom item ──
    fun startCustomItem() {
        _state.value = _state.value.copy(creatingCustomItem = true, customItemName = _state.value.productSearch, customItemPrice = "")
    }
    fun cancelCustomItem() { _state.value = _state.value.copy(creatingCustomItem = false) }
    fun setCustomName(v: String) { _state.value = _state.value.copy(customItemName = v) }
    fun setCustomPrice(v: String) { _state.value = _state.value.copy(customItemPrice = v) }
    fun submitCustomItem() {
        val name = _state.value.customItemName.trim()
        if (name.isBlank()) { _state.value = _state.value.copy(feedback = "請輸入名稱", feedbackError = true); return }
        val price = _state.value.customItemPrice.toDoubleOrNull() ?: 0.0
        val newItem = EditableItem(
            id = 0,
            productId = 0,
            skuCode = "CUSTOM",
            productName = name,
            qty = 1,
            unitPrice = price,
            originalQty = 0,
            originalUnitPrice = 0.0
        )
        _state.value = _state.value.copy(
            editItems = _state.value.editItems + newItem,
            creatingCustomItem = false,
            pickingProduct = false,
            feedback = "已加入: $name"
        )
    }

    // ── Submit ──
    fun saveChanges() {
        val st = _state.value
        val detail = st.detail ?: return
        viewModelScope.launch {
            _state.value = st.copy(saving = true, feedback = null)
            try {
                // Collect items: existing modified + new (id=0)
                val itemRequests = st.editItems
                    .filter { !it.isDeleted }
                    .map {
                        QuoteItemRequest(
                            productId = it.productId,
                            productName = if (it.productId == 0) it.productName else null,
                            qty = it.qty,
                            unitPrice = it.unitPrice
                        )
                    }

                val req = UpdateQuotationRequest(
                    customerId = st.editCustomerId,
                    warehouseId = st.editWarehouseId,
                    items = itemRequests.ifEmpty { null },
                    notes = st.editNotes.ifBlank { null }
                )

                val resp = ApiClient.service.updateQuotation(detail.id, req)
                if (resp.isSuccessful && (resp.body()?.ok ?: true)) {
                    _state.value = st.copy(saving = false, editing = false, done = true, feedback = "儲存成功")
                    // Reload detail
                    val reloadResp = ApiClient.service.getQuotationDetail(detail.id)
                    _state.value = _state.value.copy(
                        detail = reloadResp.body()?.data,
                        done = false,
                        editing = false,
                        editItems = emptyList()
                    )
                } else {
                    _state.value = st.copy(
                        saving = false,
                        feedback = resp.body()?.error?.message ?: "儲存失敗",
                        feedbackError = true
                    )
                }
            } catch (e: Exception) {
                _state.value = st.copy(saving = false, feedback = "錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}
