package com.erp.pda.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * JWT Bearer Token 自動注入攔截器
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = SessionManager.getAccessToken()

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
