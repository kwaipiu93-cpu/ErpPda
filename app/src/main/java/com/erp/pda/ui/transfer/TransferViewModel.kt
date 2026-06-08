package com.erp.pda.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.StockTransfer
import com.erp.pda.data.model.StockTransferItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferItemState(
    val item: StockTransferItem,
    val scannedSerials: MutableList<String> = mutableListOf()
) {
    val remaining: Int get() = item.qty - item.qtyReceived
    val isComplete: Boolean get() = if (item.isSerialTracked) scannedSerials.size >= remaining else true
}

data class TransferUiState(
    val transfers: List<StockTransfer> = emptyList(),
    val isLoadingList: Boolean = false,
    val selectedST: StockTransfer? = null,
    val items: List<TransferItemState> = emptyList(),
    val selectedItemIndex: Int = 0,
    val isSubmitting: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false
)

class TransferViewModel : ViewModel() {
    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state.asStateFlow()

    fun loadTransfers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingList = true)
            try {
                val resp = ApiClient.service.getStockTransfers()
                _state.value = _state.value.copy(
                    transfers = resp.body()?.data ?: emptyList(),
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

    fun selectTransfer(st: StockTransfer) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getStockTransferDetail(st.id)
                val detail = resp.body()?.data
                if (detail != null) {
                    val items = detail.items
                        .filter { it.qtyReceived < it.qty }
                        .map { TransferItemState(item = it) }
                    _state.value = _state.value.copy(selectedST = detail, items = items, selectedItemIndex = 0)
                }
            } catch (_: Exception) {}
        }
    }

    fun scanSerial(serialNumber: String): Boolean {
        val s = _state.value
        val item = s.items.getOrNull(s.selectedItemIndex) ?: return false
        if (serialNumber in item.scannedSerials) {
            _state.value = s.copy(feedback = "S/N 已掃描", feedbackError = true)
            return false
        }
        if (item.scannedSerials.size >= item.remaining) {
            _state.value = s.copy(feedback = "已達數量 (${item.remaining})", feedbackError = true)
            return false
        }
        item.scannedSerials.add(serialNumber)
        _state.value = s.copy(
            feedback = "✓ S/N: $serialNumber (${item.scannedSerials.size}/${item.remaining})",
            feedbackError = false
        )
        return true
    }

    fun selectItem(index: Int) { _state.value = _state.value.copy(selectedItemIndex = index) }

    fun confirmReceive() {
        val s = _state.value
        val st = s.selectedST ?: return
        if (!s.items.all { it.isComplete }) {
            _state.value = s.copy(feedback = "尚有商品未完成掃描", feedbackError = true)
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val serials = s.items.flatMap { it.scannedSerials }
                val resp = ApiClient.service.receiveStockTransfer(st.id, mapOf("serial_numbers" to serials))
                _state.value = if (resp.isSuccessful) {
                    s.copy(isSubmitting = false, feedback = "調撥接收成功！", feedbackError = false,
                        selectedST = null, items = emptyList())
                } else {
                    s.copy(isSubmitting = false,
                        feedback = resp.body()?.error?.message ?: "接收失敗", feedbackError = true)
                }
            } catch (e: Exception) {
                _state.value = s.copy(isSubmitting = false,
                    feedback = "網絡錯誤: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
    fun backToList() { _state.value = _state.value.copy(selectedST = null, items = emptyList()) }
}
