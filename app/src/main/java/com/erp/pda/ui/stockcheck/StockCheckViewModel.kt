package com.erp.pda.ui.stockcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.Product
import com.erp.pda.data.model.StockInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockCheckUiState(
    val searchQuery: String = "",
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val stocks: List<StockInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StockCheckViewModel : ViewModel() {
    private val _state = MutableStateFlow(StockCheckUiState())
    val state: StateFlow<StockCheckUiState> = _state.asStateFlow()

    fun searchProducts(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.searchProducts(query)
                _state.value = _state.value.copy(products = resp.body()?.data ?: emptyList(), isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun selectProduct(product: Product) {
        _state.value = _state.value.copy(selectedProduct = product)
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getProductStock(product.id)
                _state.value = _state.value.copy(stocks = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    fun clearProduct() { _state.value = _state.value.copy(selectedProduct = null, stocks = emptyList()) }
}
