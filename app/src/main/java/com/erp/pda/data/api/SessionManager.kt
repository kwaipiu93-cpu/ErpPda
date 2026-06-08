package com.erp.pda.data.api

import android.content.Context
import android.content.SharedPreferences
import com.erp.pda.data.model.LoginResponse

/**
 * Token 管理（SharedPreferences）
 */
object SessionManager {
    private const val PREFS_NAME = "erp_pda_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLogin(response: LoginResponse) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, response.accessToken)
            putString(KEY_REFRESH_TOKEN, response.refreshToken)
            response.user?.let {
                putInt(KEY_USER_ID, it.id)
                putString(KEY_USER_NAME, it.displayName)
                putString(KEY_USER_ROLE, it.role)
            }
            apply() // synchronous commit — critical for persistence
        }
    }

    /** 記住帳號密碼（退出 app 後自動登入用） */
    fun saveCredentials(email: String, password: String) {
        prefs.edit().putString(KEY_SAVED_EMAIL, email).putString(KEY_SAVED_PASSWORD, password).apply()
    }

    fun getSavedEmail(): String = prefs.getString(KEY_SAVED_EMAIL, "") ?: ""
    fun getSavedPassword(): String = prefs.getString(KEY_SAVED_PASSWORD, "") ?: ""
    fun hasSavedCredentials(): Boolean = getSavedEmail().isNotBlank() && getSavedPassword().isNotBlank()

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "") ?: ""

    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }.apply()
    }

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun logout() {
        // 只清除 token，保留帳號密碼方便下次自動登入
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }

    /** 完全清除（含帳密） */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
