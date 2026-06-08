package com.erp.pda.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.StockTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferUiState(
    val transfers: List<StockTransfer> = emptyList(),
    val isLoading: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false
)

class TransferViewModel : ViewModel() {
    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state.asStateFlow()

    fun loadTransfers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resp = ApiClient.service.getStockTransfers()
                _state.value = _state.value.copy(
                    transfers = resp.body()?.data ?: emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    feedback = "載入失敗: ${e.localizedMessage}",
                    feedbackError = true
                )
            }
        }
    }

    fun receiveTransfer(st: StockTransfer) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.receiveStockTransfer(st.id)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(feedback = "調撥接收成功！", feedbackError = false)
                    loadTransfers()
                } else {
                    _state.value = _state.value.copy(feedback = resp.body()?.error?.message ?: "接收失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}
