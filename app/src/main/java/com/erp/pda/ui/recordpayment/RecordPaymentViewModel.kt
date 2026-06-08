package com.erp.pda.ui.recordpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.model.InvoiceDetail
import com.erp.pda.data.model.InvoiceSummary
import com.erp.pda.data.model.RecordPaymentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class RecordPaymentUiState(
    val searchQuery: String = "",
    val invoices: List<InvoiceSummary> = emptyList(),
    val selectedInvoice: InvoiceDetail? = null,
    val selectedInvoiceSummary: InvoiceSummary? = null,
    val paymentAmount: String = "",
    val paymentMethod: String = "FPS",
    val referenceNumber: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class RecordPaymentViewModel : ViewModel() {
    private val _state = MutableStateFlow(RecordPaymentUiState())
    val state: StateFlow<RecordPaymentUiState> = _state.asStateFlow()

    fun searchInvoices(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _state.value = _state.value.copy(invoices = emptyList())
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getInvoices(invoiceNumber = query)
                _state.value = _state.value.copy(
                    invoices = resp.body()?.data ?: emptyList(),
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "查詢失敗"
                )
            }
        }
    }

    fun selectInvoice(inv: InvoiceSummary) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = ApiClient.service.getInvoiceDetail(inv.id)
                val detail = resp.body()?.data
                val unpaid = (detail?.grandTotalHkd ?: 0.0) - (detail?.paidAmountHkd ?: 0.0)
                _state.value = _state.value.copy(
                    selectedInvoice = detail,
                    selectedInvoiceSummary = inv,
                    paymentAmount = "%.2f".format(unpaid),
                    paymentMethod = "FPS",
                    referenceNumber = "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "載入發票詳情失敗"
                )
            }
        }
    }

    fun updatePaymentAmount(amount: String) {
        // Only allow numeric input with optional decimal point
        if (amount.isBlank() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _state.value = _state.value.copy(paymentAmount = amount)
        }
    }

    fun updatePaymentMethod(method: String) {
        _state.value = _state.value.copy(paymentMethod = method)
    }

    fun updateReferenceNumber(ref: String) {
        _state.value = _state.value.copy(referenceNumber = ref)
    }

    fun submitPayment() {
        val s = _state.value
        val inv = s.selectedInvoice ?: return
        val invSummary = s.selectedInvoiceSummary ?: return
        val amount = s.paymentAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.value = _state.value.copy(error = "請輸入有效的付款金額")
            return
        }
        val unpaid = inv.grandTotalHkd - inv.paidAmountHkd
        if (amount > unpaid) {
            _state.value = _state.value.copy(error = "付款金額不能超過未付金額 HKD ${
                "%.2f".format(unpaid)
            }")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            try {
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val req = RecordPaymentRequest(
                    customerId = invSummary.customerId,
                    amountHkd = amount,
                    paymentMethod = s.paymentMethod,
                    referenceNumber = s.referenceNumber,
                    invoiceId = inv.id,
                    receivedAt = now
                )
                ApiClient.service.recordPayment(req)
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    successMessage = "收款成功！發票 ${inv.invoiceNumber} 已記錄 HKD ${
                        "%.2f".format(amount)
                    }"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    error = e.localizedMessage ?: "提交失敗"
                )
            }
        }
    }

    fun clearSelection() {
        _state.value = _state.value.copy(
            selectedInvoice = null,
            selectedInvoiceSummary = null,
            paymentAmount = "",
            paymentMethod = "FPS",
            referenceNumber = "",
            error = null
        )
    }

    fun dismissSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun resetForm() {
        _state.value = RecordPaymentUiState()
    }
}
