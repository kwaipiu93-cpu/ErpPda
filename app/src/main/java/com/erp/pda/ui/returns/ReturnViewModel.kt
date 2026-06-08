package com.erp.pda.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.CreditNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReturnUiState(
    val creditNotes: List<CreditNote> = emptyList(),
    val isLoading: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false
)

class ReturnViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReturnUiState())
    val state: StateFlow<ReturnUiState> = _state.asStateFlow()

    fun loadCreditNotes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resp = ApiClient.service.getCreditNotes()
                _state.value = _state.value.copy(
                    creditNotes = resp.body()?.data ?: emptyList(),
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

    fun confirmReturn(cn: CreditNote) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.confirmCreditNote(cn.id)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(feedback = "退貨確認成功！", feedbackError = false)
                    loadCreditNotes()
                } else {
                    _state.value = _state.value.copy(feedback = resp.body()?.error?.message ?: "確認失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}
