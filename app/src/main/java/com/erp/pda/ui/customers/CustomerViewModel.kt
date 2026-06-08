package com.erp.pda.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.CustomerDetail
import com.erp.pda.data.model.CustomerSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerUiState(
    val searchQuery: String = "",
    val searchResults: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isViewingDetail: Boolean = false
)

class CustomerViewModel : ViewModel() {
    private val _state = MutableStateFlow(CustomerUiState())
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query, error = null)
        if (query.length >= 2) {
            search()
        } else if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
        }
    }

    fun search() {
        val query = _state.value.searchQuery.trim()
        if (query.isBlank()) {
            _state.value = _state.value.copy(error = "請輸入客戶名稱")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.searchCustomers(query)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        searchResults = resp.body()?.data ?: emptyList()
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = resp.body()?.error?.message ?: "查無客戶"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "網絡錯誤: ${e.localizedMessage}"
                )
            }
        }
    }

    fun viewDetail(customerId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getCustomerDetail(customerId)
                if (resp.isSuccessful && resp.body()?.ok == true) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        selectedCustomer = resp.body()?.data,
                        isViewingDetail = true
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = resp.body()?.error?.message ?: "無法載入客戶資料"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "網絡錯誤: ${e.localizedMessage}"
                )
            }
        }
    }

    fun backToList() {
        _state.value = _state.value.copy(
            isViewingDetail = false,
            selectedCustomer = null,
            error = null
        )
    }

    fun setSearchAndSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        search()
    }
}
