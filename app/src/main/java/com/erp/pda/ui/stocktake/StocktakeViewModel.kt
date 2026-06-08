package com.erp.pda.ui.stocktake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.StocktakeRequest
import com.erp.pda.data.model.StocktakeScanItem
import com.erp.pda.data.model.StocktakeTask
import com.erp.pda.data.model.Warehouse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StocktakeUiState(
    val warehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: Int? = null,
    val activeTask: StocktakeTask? = null,
    val scannedCount: Int = 0,
    val lastScanned: String? = null,
    val isLoading: Boolean = false,
    val feedback: String? = null,
    val feedbackError: Boolean = false
)

class StocktakeViewModel : ViewModel() {
    private val _state = MutableStateFlow(StocktakeUiState())
    val state: StateFlow<StocktakeUiState> = _state.asStateFlow()

    fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service.getWarehouses()
                _state.value = _state.value.copy(warehouses = resp.body()?.data ?: emptyList())
            } catch (_: Exception) {}
        }
    }

    fun selectWarehouse(id: Int) {
        _state.value = _state.value.copy(selectedWarehouseId = id)
    }

    fun createTask() {
        val wid = _state.value.selectedWarehouseId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resp = ApiClient.service.createStocktake(StocktakeRequest(warehouseId = wid, remarks = "PDA 盤點"))
                val task = resp.body()?.data
                if (task != null) {
                    _state.value = _state.value.copy(activeTask = task, scannedCount = 0, isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, feedback = "建立失敗: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun scanBarcode(code: String) {
        val task = _state.value.activeTask ?: return
        viewModelScope.launch {
            try {
                ApiClient.service.scanStocktake(task.id, StocktakeScanItem(serialNumber = code))
                val s = _state.value
                _state.value = s.copy(
                    scannedCount = s.scannedCount + 1,
                    lastScanned = code,
                    feedback = "✓ $code (${s.scannedCount + 1})",
                    feedbackError = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(feedback = "掃描失敗: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun completeTask() {
        val task = _state.value.activeTask ?: return
        viewModelScope.launch {
            try {
                ApiClient.service.completeStocktake(task.id)
                _state.value = _state.value.copy(activeTask = null, feedback = "盤點完成！", feedbackError = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(feedback = "完成失敗: ${e.localizedMessage}", feedbackError = true)
            }
        }
    }

    fun clearFeedback() { _state.value = _state.value.copy(feedback = null) }
}
