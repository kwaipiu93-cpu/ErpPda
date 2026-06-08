package com.erp.pda.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erp.pda.data.api.ApiClient
import com.erp.pda.data.api.SessionManager
import com.erp.pda.data.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        // App 啟動時自動檢查是否已登入
        if (SessionManager.isLoggedIn()) {
            _state.value = _state.value.copy(isLoading = true)
            tryAutoLogin()
        }
    }

    /** 嘗試用現有 token 自動登入（session 有效則免重入密碼） */
    private fun tryAutoLogin() {
        viewModelScope.launch {
            try {
                // 試 refresh token 確保有效
                val refreshToken = SessionManager.getRefreshToken()
                if (!refreshToken.isNullOrBlank()) {
                    val resp = ApiClient.service.refreshToken(
                        com.erp.pda.data.model.TokenRefreshRequest(refreshToken)
                    )
                    if (resp.isSuccessful && resp.body()?.ok == true) {
                        resp.body()?.data?.let {
                            SessionManager.updateTokens(it.accessToken, it.refreshToken)
                        }
                    }
                }
                // 有 token 就直接登入（refresh 失敗都照入，靠 401 interceptor 處理）
                _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
            } catch (_: Exception) {
                // 有 token 就照入，網絡錯誤唔應該阻住
                _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
            }
        }
    }

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "請輸入電郵和密碼")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.service.login(LoginRequest(s.email, s.password))
                if (response.isSuccessful && response.body()?.ok == true) {
                    response.body()?.data?.let { data ->
                        SessionManager.saveLogin(data)
                        _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                        return@launch
                    }
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = response.body()?.error?.message ?: "登入失敗"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "網絡錯誤: ${e.localizedMessage}"
                )
            }
        }
    }
}
