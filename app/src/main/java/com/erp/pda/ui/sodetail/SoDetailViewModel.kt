package com.erp.pda.ui.sodetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.SalesOrderDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SoDetailUiState(
    val detail: SalesOrderDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SoDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(SoDetailUiState())
    val state: StateFlow<SoDetailUiState> = _state.asStateFlow()

    fun loadDetail(id: Int) {
        viewModelScope.launch {
            _state.value = SoDetailUiState(isLoading = true)
            try {
                val resp = ApiClient.service.getSalesOrder(id)
                _state.value = SoDetailUiState(detail = resp.body()?.data)
            } catch (e: Exception) {
                _state.value = SoDetailUiState(error = e.localizedMessage)
            }
        }
    }
}
