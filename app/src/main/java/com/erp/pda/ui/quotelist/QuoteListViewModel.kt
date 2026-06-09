package com.erp.pda.ui.quotelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.InvoiceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuoteListUiState(
    val quotations: List<InvoiceSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class QuoteListViewModel : ViewModel() {
    private val _state = MutableStateFlow(QuoteListUiState())
    val state: StateFlow<QuoteListUiState> = _state.asStateFlow()

    init { loadQuotations() }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        loadQuotations()
    }

    fun loadQuotations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getQuotations()
                _state.value = _state.value.copy(
                    quotations = resp.body()?.data ?: emptyList(),
                    isLoading = false,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false, error = e.localizedMessage)
            }
        }
    }
}
