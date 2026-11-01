package com.example.huetaste.data.remote

import com.example.huetaste.data.local.SecureSessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adds the signed-in user's access token to every backend request; guests send none. */
class AuthInterceptor @Inject constructor(private val store: SecureSessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = store.session()?.accessToken
        if (token == null || request.header(AUTHORIZATION) != null) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header(AUTHORIZATION, "$BEARER $token").build())
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"
    }
}
