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
    val error: String? = null,
    val actionMessage: String? = null
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

    fun updateQuoteStatus(id: Int, action: QuoteAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    QuoteAction.SEND -> ApiClient.service.sendQuotation(id)
                    QuoteAction.VOID -> ApiClient.service.voidQuotation(id)
                    QuoteAction.ACCEPT -> ApiClient.service.acceptQuotation(id)
                    QuoteAction.REOPEN -> ApiClient.service.reopenQuotation(id)
                    QuoteAction.REVERT_DRAFT -> ApiClient.service.revertQuotationToDraft(id)
                }
                _state.value = _state.value.copy(actionMessage = action.successMessage)
                loadQuotations()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "${action.label} 失败: ${e.localizedMessage}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}

enum class QuoteAction(val label: String, val successMessage: String) {
    SEND("發送", "報價單已發送"),
    VOID("作廢", "報價單已作廢"),
    ACCEPT("接受", "報價單已接受"),
    REOPEN("重開", "報價單已重開"),
    REVERT_DRAFT("退回草稿", "已退回草稿")
}
