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
    val isLoggedIn: Boolean = false,
    val autoLoginAttempted: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        val savedEmail = SessionManager.getSavedEmail()
        val savedPassword = SessionManager.getSavedPassword()

        if (SessionManager.isLoggedIn()) {
            // 有 token → refresh + 直入
            _state.value = LoginUiState(
                email = savedEmail,
                password = savedPassword,
                isLoading = true
            )
            tryAutoLogin()
        } else if (SessionManager.hasSavedCredentials()) {
            // 無 token 但有記住密碼 → 自動登入
            _state.value = LoginUiState(
                email = savedEmail,
                password = savedPassword,
                isLoading = true,
                autoLoginAttempted = true
            )
            loginWithCredentials(savedEmail, savedPassword)
        } else {
            // 首次使用 → 顯示登入頁面（預填 email 如有）
            _state.value = LoginUiState(email = savedEmail)
        }
    }

    private fun tryAutoLogin() {
        viewModelScope.launch {
            try {
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
                _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
            }
        }
    }

    /** 用儲存的帳密自動登入 */
    private fun loginWithCredentials(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.service.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.ok == true) {
                    response.body()?.data?.let { data ->
                        SessionManager.saveLogin(data)
                        SessionManager.saveCredentials(email, password)
                        _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                        return@launch
                    }
                }
                // 登入失敗 → 顯示表單（預填帳密）
                _state.value = LoginUiState(
                    email = email,
                    password = "",
                    error = response.body()?.error?.message ?: "自動登入失敗，請重新輸入密碼"
                )
            } catch (e: Exception) {
                _state.value = LoginUiState(
                    email = email,
                    password = "",
                    error = "網絡錯誤: ${e.localizedMessage}"
                )
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
                        SessionManager.saveCredentials(s.email, s.password)
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
