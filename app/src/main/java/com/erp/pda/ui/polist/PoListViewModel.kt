package com.erp.pda.ui.polist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.PurchaseOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PoFilter(val label: String, val apiStatus: String?) {
    ALL("全部", null),
    PENDING_RECEIVE("待收貨", "Ordered,Partially_Received"),
    RECEIVED("已收貨", "Received")
}

data class PoListUiState(
    val purchaseOrders: List<PurchaseOrder> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val activeFilter: PoFilter = PoFilter.ALL
)

class PoListViewModel : ViewModel() {
    private val _state = MutableStateFlow(PoListUiState())
    val state: StateFlow<PoListUiState> = _state.asStateFlow()

    init {
        loadPurchaseOrders()
    }

    fun loadPurchaseOrders() {
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(isLoading = true, error = null)
            try {
                val statusParam = current.activeFilter.apiStatus
                    ?: "Ordered,Partially_Received,Received"
                val resp = ApiClient.service.getPurchaseOrders(status = statusParam)
                val data = resp.body()?.data ?: emptyList()
                _state.value = _state.value.copy(
                    purchaseOrders = data,
                    isLoading = false,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.localizedMessage
                )
            }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        loadPurchaseOrders()
    }

    fun setFilter(filter: PoFilter) {
        if (filter == _state.value.activeFilter) return
        _state.value = _state.value.copy(activeFilter = filter, purchaseOrders = emptyList())
        loadPurchaseOrders()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
