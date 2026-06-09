package com.erp.pda.ui.invoicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.InvoiceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class InvoiceFilter(val label: String, val apiStatus: String?) {
    ALL("全部", null),
    UNPAID("未付", "Unpaid"),
    PAID("已付", "Paid")
}

data class InvoiceListUiState(
    val invoices: List<InvoiceSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val activeFilter: InvoiceFilter = InvoiceFilter.ALL,
    val salesOnly: Boolean = false
)

class InvoiceListViewModel : ViewModel() {
    private val _state = MutableStateFlow(InvoiceListUiState())
    val state: StateFlow<InvoiceListUiState> = _state.asStateFlow()

    private var _salesOnly = false

    fun setSalesOnly(v: Boolean) {
        if (_salesOnly != v) {
            _salesOnly = v
            _state.value = _state.value.copy(salesOnly = v, invoices = emptyList())
            loadInvoices()
        }
    }

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getInvoices(
                    status = current.activeFilter.apiStatus,
                    docType = "INV"
                )
                var data = resp.body()?.data ?: emptyList()
                // Sales orders: only Active lifecycle
                if (_salesOnly) {
                    data = data.filter { it.lifecycleStatus == "Active" }
                }
                _state.value = _state.value.copy(
                    invoices = data,
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
        loadInvoices()
    }

    fun setFilter(filter: InvoiceFilter) {
        if (filter == _state.value.activeFilter) return
        _state.value = _state.value.copy(activeFilter = filter, invoices = emptyList())
        loadInvoices()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
