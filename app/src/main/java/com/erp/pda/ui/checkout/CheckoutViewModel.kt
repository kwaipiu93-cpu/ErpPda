package com.erp.pda.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    var qty: Int = 1,
    val scannedSerials: MutableList<String> = mutableListOf()
) {
    val lineTotal: Double get() = qty * 0.0 // price set from product
    val isSerialComplete: Boolean get() = if (product.isSerialTracked) scannedSerials.size >= qty else true
}

enum class CheckoutStep { CART, CUSTOMER, PAYMENT, DONE }

data class CheckoutUiState(
    val step: CheckoutStep = CheckoutStep.CART,
    // Cart
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val selectedCartIndex: Int = 0,
    // Customer
    val customerSearch: String = "",
    val customerResults: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerSummary? = null,
    // Payment
    val paymentMethod: String = "FPS",
    val referenceNumber: String = "",
    // Warehouse
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int = 1,
    // Status
    val isLoading: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false,
    // Result
    val resultInvoice: String? = null,
    val resultTotal: Double = 0.0
)

class CheckoutViewModel : ViewModel() {
    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getWarehouses()
                _state.value = _state.value.copy(warehouses = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    // ─── Step 1: Cart ───
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

    fun addToCart(product: Product) {
        val s = _state.value
        val existing = s.cartItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.qty++
            _state.value = s.copy(feedback = "${product.skuCode} x${existing.qty}", feedbackError = false)
        } else {
            val newCart = s.cartItems + CartItem(product = product)
            _state.value = s.copy(cartItems = newCart, feedback = "已加入: ${product.skuCode}", feedbackError = false,
                selectedCartIndex = newCart.size - 1)
        }
    }

    fun scanBarcode(code: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchProducts(code)
                val products = resp.body()?.data ?: emptyList()
                if (products.size == 1) {
                    addToCart(products.first())
                } else if (products.isNotEmpty()) {
                    _state.value = _state.value.copy(searchResults = products, searchQuery = code)
                } else {
                    _state.value = _state.value.copy(feedback = "找不到商品: $code", feedbackError = true)
                }
            } catch (_: Exception) {}
        }
    }

    fun scanSerialForCart(serialNumber: String): Boolean {
        val s = _state.value
        val item = s.cartItems.getOrNull(s.selectedCartIndex) ?: return false
        if (!item.product.isSerialTracked) {
            _state.value = s.copy(feedback = "此商品不需 S/N", feedbackError = true)
            return false
        }
        if (serialNumber in item.scannedSerials) {
            _state.value = s.copy(feedback = "S/N 已掃描", feedbackError = true)
            return false
        }
        if (item.scannedSerials.size >= item.qty) {
            _state.value = s.copy(feedback = "S/N 數量已達", feedbackError = true)
            return false
        }
        item.scannedSerials.add(serialNumber)
        _state.value = s.copy(feedback = "✓ S/N: $serialNumber (${item.scannedSerials.size}/${item.qty})", feedbackError = false)
        return true
    }

    fun selectCartItem(index: Int) { _state.value = _state.value.copy(selectedCartIndex = index) }
    fun removeCartItem(index: Int) {
        val items = _state.value.cartItems.toMutableList()
        items.removeAt(index)
        _state.value = _state.value.copy(cartItems = items, selectedCartIndex = 0)
    }
    fun adjustQty(index: Int, delta: Int) {
        val s = _state.value
        val item = s.cartItems.getOrNull(index) ?: return
        item.qty = (item.qty + delta).coerceAtLeast(1)
        _state.value = s.copy(cartItems = s.cartItems.toList())
    }

    fun goToCustomer() {
        if (_state.value.cartItems.isEmpty()) {
            _state.value = _state.value.copy(feedback = "請先加入商品", feedbackError = true)
            return
        }
        _state.value = _state.value.copy(step = CheckoutStep.CUSTOMER)
    }

    // ─── Step 2: Customer ───
    fun searchCustomers(query: String) {
        _state.value = _state.value.copy(customerSearch = query)
        if (query.length < 2) return
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.searchCustomers(query)
                val results = resp.body()?.data ?: emptyList()
                // Filter B2C only
                _state.value = _state.value.copy(customerResults = results.filter { it.customerType == "B2C" })
            } catch (_: Exception) {}
        }
    }

    fun selectCustomer(c: CustomerSummary) {
        _state.value = _state.value.copy(selectedCustomer = c)
    }
    fun goToPayment() {
        if (_state.value.selectedCustomer == null) {
            _state.value = _state.value.copy(feedback = "請選擇客戶", feedbackError = true)
            return
        }
        _state.value = _state.value.copy(step = CheckoutStep.PAYMENT)
    }

    // ─── Step 3: Payment ───
    fun setPaymentMethod(method: String) { _state.value = _state.value.copy(paymentMethod = method) }
    fun setReferenceNumber(ref: String) { _state.value = _state.value.copy(referenceNumber = ref) }
    fun selectWarehouse(id: Int) { _state.value = _state.value.copy(selectedWarehouseId = id) }

    fun submitCheckout() {
        val s = _state.value
        val cust = s.selectedCustomer ?: return

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            try {
                val items = s.cartItems.map { ci ->
                    B2cCheckoutItem(
                        productId = ci.product.id,
                        qty = ci.qty,
                        unitPrice = 0.0, // backend determines price from product
                        serialNumbers = ci.scannedSerials.toList()
                    )
                }
                val req = B2cCheckoutRequest(
                    customerId = cust.id,
                    warehouseId = s.selectedWarehouseId,
                    paymentMethod = s.paymentMethod,
                    referenceNumber = s.referenceNumber,
                    items = items
                )
                val resp = ApiClient.service.b2cFastCheckout(req)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    val data = resp.body()?.data
                    _state.value = s.copy(
                        isLoading = false,
                        step = CheckoutStep.DONE,
                        resultInvoice = data?.invoiceNumber ?: "",
                        resultTotal = data?.totalHkd ?: 0.0
                    )
                } else {
                    _state.value = s.copy(isLoading = false, feedback = resp.body()?.error?.message ?: "結帳失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = s.copy(isLoading = false, feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun newTransaction() {
        _state.value = CheckoutUiState(warehouses = _state.value.warehouses, selectedWarehouseId = _state.value.selectedWarehouseId)
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
    fun backToCart() { _state.value = _state.value.copy(step = CheckoutStep.CART) }
    fun backToCustomer() { _state.value = _state.value.copy(step = CheckoutStep.CUSTOMER) }
}
