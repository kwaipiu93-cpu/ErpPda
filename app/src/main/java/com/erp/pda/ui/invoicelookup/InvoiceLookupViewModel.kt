package com.erp.pda.ui.invoicelookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.InvoiceDetail
import com.erp.pda.data.model.InvoiceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InvoiceLookupUiState(
    val searchQuery: String = "",
    val invoices: List<InvoiceSummary> = emptyList(),
    val selectedInvoice: InvoiceDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class InvoiceLookupViewModel : ViewModel() {
    private val _state = MutableStateFlow(InvoiceLookupUiState())
    val state: StateFlow<InvoiceLookupUiState> = _state.asStateFlow()

    fun searchInvoices(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getInvoices(invoiceNumber = query)
                _state.value = _state.value.copy(
                    invoices = resp.body()?.data ?: emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun selectInvoice(inv: InvoiceSummary) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resp = ApiClient.service.getInvoiceDetail(inv.id)
                _state.value = _state.value.copy(
                    selectedInvoice = resp.body()?.data,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun clearSelection() { _state.value = _state.value.copy(selectedInvoice = null) }
}
