package com.erp.pda.ui.dispatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.DeliveryNote
import com.erp.pda.data.model.DeliveryNoteDetail
import com.erp.pda.data.model.DeliveryNoteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DispatchItemState(
    val dnItem: DeliveryNoteItem,
    val scannedSerials: MutableList<String> = mutableListOf()
) {
    val remaining: Int get() = dnItem.qtyToDeliver - dnItem.qtyDelivered
    val isComplete: Boolean get() = if (dnItem.isSerialTracked) {
        scannedSerials.size >= remaining
    } else true
}

data class DispatchUiState(
    val deliveryNotes: List<DeliveryNote> = emptyList(),
    val isLoadingList: Boolean = false,
    val selectedDN: DeliveryNoteDetail? = null,
    val isLoadingDetail: Boolean = false,
    val items: List<DispatchItemState> = emptyList(),
    val selectedItemIndex: Int = 0,
    val isSubmitting: Boolean = false,
    val lastFeedback: String? = null,
    val feedbackIsError: Boolean = false
)

class DispatchViewModel : ViewModel() {
    private val _state = MutableStateFlow(DispatchUiState())
    val state: StateFlow<DispatchUiState> = _state.asStateFlow()

    fun loadDeliveryNotes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingList = true)
            try {
                val resp = ApiClient.service.getDeliveryNotes()
                _state.value = _state.value.copy(
                    deliveryNotes = resp.body()?.data ?: emptyList(),
                    isLoadingList = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingList = false,
                    lastFeedback = "載入失敗: ${e.localizedMessage}",
                    feedbackIsError = true
                )
            }
        }
    }

    fun selectDN(dn: DeliveryNote) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingDetail = true)
            try {
                val resp = ApiClient.service.getDeliveryNote(dn.id)
                val detail = resp.body()?.data
                if (detail != null) {
                    val items = detail.items
                        .filter { it.qtyDelivered < it.qtyToDeliver }
                        .map { DispatchItemState(dnItem = it) }
                    _state.value = _state.value.copy(
                        selectedDN = detail,
                        items = items,
                        isLoadingDetail = false,
                        selectedItemIndex = 0
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingDetail = false,
                    lastFeedback = "載入詳情失敗: ${e.localizedMessage}",
                    feedbackIsError = true
                )
            }
        }
    }

    fun scanSerial(serialNumber: String): Boolean {
        val s = _state.value
        val item = s.items.getOrNull(s.selectedItemIndex) ?: return false
        if (serialNumber in item.scannedSerials) {
            _state.value = s.copy(lastFeedback = "S/N 已掃描", feedbackIsError = true)
            return false
        }
        if (item.scannedSerials.size >= item.remaining) {
            _state.value = s.copy(lastFeedback = "已達所需數量", feedbackIsError = true)
            return false
        }
        item.scannedSerials.add(serialNumber)
        _state.value = s.copy(
            lastFeedback = "✓ S/N: $serialNumber (${item.scannedSerials.size}/${item.remaining})",
            feedbackIsError = false
        )
        return true
    }

    fun selectItem(index: Int) {
        _state.value = _state.value.copy(selectedItemIndex = index)
    }

    fun submitDispatch() {
        val s = _state.value
        val dn = s.selectedDN ?: return
        if (!s.items.all { it.isComplete }) {
            _state.value = s.copy(lastFeedback = "尚有商品未完成掃描", feedbackIsError = true)
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val resp = ApiClient.service.dispatchDeliveryNote(dn.id, mapOf("warehouse_id" to 1))
                _state.value = if (resp.isSuccessful) {
                    s.copy(isSubmitting = false, lastFeedback = "出貨成功！", feedbackIsError = false, selectedDN = null, items = emptyList())
                } else {
                    s.copy(isSubmitting = false, lastFeedback = resp.body()?.error?.message ?: "出貨失敗", feedbackIsError = true)
                }
            } catch (e: Exception) {
                _state.value = s.copy(isSubmitting = false, lastFeedback = "網絡錯誤: ${e.localizedMessage}", feedbackIsError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(lastFeedback = null) }
    fun backToList() { _state.value = _state.value.copy(selectedDN = null, items = emptyList()) }
}
