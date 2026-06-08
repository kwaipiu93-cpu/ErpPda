package com.erp.pda.ui.receiving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.PurchaseOrder
import com.erp.pda.data.model.PurchaseOrderDetail
import com.erp.pda.data.model.PurchaseOrderItem
import com.erp.pda.data.model.ReceiveItem
import com.erp.pda.data.model.ReceiveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReceivingItemState(
    val poItem: PurchaseOrderItem,
    val scannedSerials: MutableList<String> = mutableListOf()
) {
    val remaining: Int get() = poItem.qtyOrdered - poItem.qtyReceived
    val isComplete: Boolean get() = if (poItem.isSerialTracked) {
        scannedSerials.size >= remaining
    } else true
}

data class ReceivingUiState(
    // PO list
    val purchaseOrders: List<PurchaseOrder> = emptyList(),
    val isLoadingPOs: Boolean = false,
    // Selected PO detail
    val selectedPO: PurchaseOrderDetail? = null,
    val isLoadingDetail: Boolean = false,
    // Receiving state
    val items: List<ReceivingItemState> = emptyList(),
    val selectedItemIndex: Int = 0,
    // Submission
    val isSubmitting: Boolean = false,
    val lastFeedback: String? = null,
    val feedbackIsError: Boolean = false,
    // Warehouse
    val warehouseId: Int = 0
)

class ReceivingViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReceivingUiState())
    val state: StateFlow<ReceivingUiState> = _state.asStateFlow()

    fun loadPurchaseOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingPOs = true)
            try {
                val resp = ApiClient.service.getPurchaseOrders()
                val data = resp.body()?.data ?: emptyList()
                _state.value = _state.value.copy(purchaseOrders = data, isLoadingPOs = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingPOs = false,
                    lastFeedback = "載入PO失敗: ${e.localizedMessage}",
                    feedbackIsError = true
                )
            }
        }
    }

    fun selectPO(po: PurchaseOrder) {
        _state.value = _state.value.copy(warehouseId = po.warehouseId)
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingDetail = true)
            try {
                val resp = ApiClient.service.getPurchaseOrder(po.id)
                val detail = resp.body()?.data
                if (detail != null) {
                    val items = detail.items
                        .filter { it.qtyReceived < it.qtyOrdered }
                        .map { ReceivingItemState(poItem = it) }
                    _state.value = _state.value.copy(
                        selectedPO = detail,
                        items = items,
                        isLoadingDetail = false,
                        selectedItemIndex = 0
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoadingDetail = false,
                        lastFeedback = "無法載入PO詳情",
                        feedbackIsError = true
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

        // 重複檢查
        if (serialNumber in item.scannedSerials) {
            _state.value = s.copy(
                lastFeedback = "S/N $serialNumber 已掃描，跳過",
                feedbackIsError = true
            )
            return false
        }

        // 數量檢查
        if (item.scannedSerials.size >= item.remaining) {
            _state.value = s.copy(
                lastFeedback = "已達所需數量 (${item.remaining})",
                feedbackIsError = true
            )
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

    fun submitReceive() {
        val s = _state.value
        val po = s.selectedPO ?: return

        // 檢查所有非 S/N 追蹤商品已處理
        val allReady = s.items.all { it.isComplete }
        if (!allReady) {
            _state.value = s.copy(
                lastFeedback = "尚有商品未完成掃描",
                feedbackIsError = true
            )
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            try {
                val receiveItems = s.items.map { item ->
                    ReceiveItem(
                        poItemId = item.poItem.id,
                        productId = item.poItem.productId,
                        qtyReceived = item.remaining,
                        serialNumbers = item.scannedSerials.toList()
                    )
                }
                val request = ReceiveRequest(
                    warehouseId = s.warehouseId,
                    items = receiveItems
                )
                val resp = ApiClient.service.receivePurchaseOrder(po.id, request)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        lastFeedback = "收貨成功！",
                        feedbackIsError = false,
                        selectedPO = null,
                        items = emptyList()
                    )
                } else {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        lastFeedback = resp.body()?.error?.message ?: "收貨失敗",
                        feedbackIsError = true
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    lastFeedback = "網絡錯誤: ${e.localizedMessage}",
                    feedbackIsError = true
                )
            }
        }
    }

    fun clearFeedback() {
        _state.value = _state.value.copy(lastFeedback = null)
    }

    fun backToList() {
        _state.value = _state.value.copy(selectedPO = null, items = emptyList())
    }
}
