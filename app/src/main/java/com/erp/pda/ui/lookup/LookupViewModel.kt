package com.erp.pda.ui.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.SerialLookupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LookupUiState(
    val serialNumber: String = "",
    val result: SerialLookupResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LookupViewModel : ViewModel() {
    private val _state = MutableStateFlow(LookupUiState())
    val state: StateFlow<LookupUiState> = _state.asStateFlow()

    fun onSerialChange(sn: String) {
        _state.value = _state.value.copy(serialNumber = sn, error = null)
    }

    fun lookup() {
        val sn = _state.value.serialNumber.trim()
        if (sn.isBlank()) {
            _state.value = _state.value.copy(error = "請輸入或掃描 S/N")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.lookupSerial(sn)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    _state.value = _state.value.copy(isLoading = false, result = resp.body()?.data)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = resp.body()?.error?.message ?: "查無此 S/N"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "網絡錯誤: ${e.localizedMessage}")
            }
        }
    }

    fun setSerialAndLookup(sn: String) {
        _state.value = _state.value.copy(serialNumber = sn)
        lookup()
    }
}
