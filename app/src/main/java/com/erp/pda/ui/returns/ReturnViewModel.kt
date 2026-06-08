package com.erp.pda.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.CreditNote
import com.erp.pda.data.model.CreditNoteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReturnItemState(
    val item: CreditNoteItem,
    val scannedSerials: MutableList<String> = mutableListOf(),
    var isDefective: Boolean = false
) {
    val isComplete: Boolean get() = if (item.isSerialTracked) scannedSerials.size >= item.qty else true
}

data class ReturnUiState(
    // List
    val creditNotes: List<CreditNote> = emptyList(),
    val isLoadingList: Boolean = false,
    // Detail
    val selectedCN: CreditNote? = null,
    val items: List<ReturnItemState> = emptyList(),
    val selectedItemIndex: Int = 0,
    // Submission
    val isSubmitting: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false
)

class ReturnViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReturnUiState())
    val state: StateFlow<ReturnUiState> = _state.asStateFlow()

    fun loadCreditNotes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingList = true)
            try {
                val resp = ApiClient.service.getCreditNotes()
                _state.value = _state.value.copy(
                    creditNotes = resp.body()?.data ?: emptyList(),
                    isLoadingList = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingList = false,
                    feedback = "載入失敗: ${e.localizedMessage}",
                    feedbackError = true
                )
            }
        }
    }

    fun selectCreditNote(cn: CreditNote) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getCreditNoteDetail(cn.id)
                val detail = resp.body()?.data
                if (detail != null) {
                    val returnItems = detail.items.map { ReturnItemState(item = it) }
                    _state.value = _state.value.copy(
                        selectedCN = detail,
                        items = returnItems,
                        selectedItemIndex = 0
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun scanSerial(serialNumber: String): Boolean {
        val s = _state.value
        val item = s.items.getOrNull(s.selectedItemIndex) ?: return false

        if (serialNumber in item.scannedSerials) {
            _state.value = s.copy(feedback = "S/N $serialNumber 已掃描", feedbackError = true)
            return false
        }
        if (item.scannedSerials.size >= item.item.qty) {
            _state.value = s.copy(feedback = "已達退貨數量 (${item.item.qty})", feedbackError = true)
            return false
        }
        item.scannedSerials.add(serialNumber)
        _state.value = s.copy(
            feedback = "✓ 退貨 S/N: $serialNumber (${item.scannedSerials.size}/${item.item.qty})",
            feedbackError = false
        )
        return true
    }

    fun selectItem(index: Int) { _state.value = _state.value.copy(selectedItemIndex = index) }
    fun toggleDefective() {
        val s = _state.value
        val item = s.items.getOrNull(s.selectedItemIndex) ?: return
        item.isDefective = !item.isDefective
        _state.value = s.copy(feedback = if (item.isDefective) "已標記瑕疵品" else "已標記良品", feedbackError = false)
    }

    fun confirmReturn() {
        val s = _state.value
        val cn = s.selectedCN ?: return

        if (!s.items.all { it.isComplete }) {
            _state.value = s.copy(feedback = "尚有商品未完成掃描", feedbackError = true)
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val serials = s.items.flatMap { it.scannedSerials }
                val defectiveIndices = s.items.mapIndexedNotNull { idx, item ->
                    if (item.isDefective && item.item.isSerialTracked) idx else null
                }
                val body = mapOf(
                    "serial_numbers" to serials,
                    "defective_items" to defectiveIndices
                )
                val resp = ApiClient.service.confirmCreditNote(cn.id, body)
                _state.value = if (resp.isSuccessful) {
                    s.copy(isSubmitting = false, feedback = "退貨確認成功！", feedbackError = false,
                        selectedCN = null, items = emptyList())
                } else {
                    s.copy(isSubmitting = false,
                        feedback = resp.body()?.error?.message ?: "確認失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = s.copy(isSubmitting = false,
                    feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
    fun backToList() { _state.value = _state.value.copy(selectedCN = null, items = emptyList()) }
}
