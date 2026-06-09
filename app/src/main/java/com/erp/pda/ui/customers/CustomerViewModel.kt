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
    val allCustomers: List<CustomerSummary> = emptyList(),
    val searchResults: List<CustomerSummary> = emptyList(),
    val selectedCustomer: CustomerDetail? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isViewingDetail: Boolean = false
) {
    /** 當前顯示的列表（搜尋中顯示結果，否則顯示全部） */
    val displayedCustomers: List<CustomerSummary>
        get() = if (searchQuery.isBlank()) allCustomers else searchResults
}

class CustomerViewModel : ViewModel() {
    private val _state = MutableStateFlow(CustomerUiState())
    val state: StateFlow<CustomerUiState> = _state.asStateFlow()

    init {
        loadAllCustomers()
    }

    /** 下拉刷新 */
    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        loadAllCustomers()
    }

    /** 載入全部客戶 */
    fun loadAllCustomers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // 嘗試 GET /customers，失敗則用 search?q= 空字串 fallback
                var resp = ApiClient.service.getAllCustomers()
                if (!resp.isSuccessful) {
                    resp = ApiClient.service.searchCustomers("")
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    allCustomers = resp.body()?.data ?: emptyList()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "載入失敗: ${e.localizedMessage}"
                )
            }
        }
    }

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
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.searchCustomers(query)
                _state.value = _state.value.copy(
                    isLoading = false,
                    searchResults = resp.body()?.data ?: emptyList()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "搜尋失敗: ${e.localizedMessage}"
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
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "載入失敗: ${e.localizedMessage}"
                )
            }
        }
    }

    fun backToList() {
        _state.value = _state.value.copy(isViewingDetail = false, selectedCustomer = null, error = null)
    }

    fun setSearchAndSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        search()
    }
}
