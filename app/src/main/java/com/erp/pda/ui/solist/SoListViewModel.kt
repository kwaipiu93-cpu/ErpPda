package com.erp.pda.ui.solist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.SalesOrderSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SoListUiState(
    val orders: List<SalesOrderSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class SoListViewModel : ViewModel() {
    private val _state = MutableStateFlow(SoListUiState())
    val state: StateFlow<SoListUiState> = _state.asStateFlow()

    init { loadOrders() }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getSalesOrders()
                _state.value = _state.value.copy(
                    orders = resp.body()?.data ?: emptyList(),
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
}
