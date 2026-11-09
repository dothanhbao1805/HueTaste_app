package com.example.huetaste.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Gives a chat question more time than other requests. One answer can take up to 4 model rounds of 30 seconds each on
 * the backend; raise this if `GEMINI_MODEL_CHAT` lists more than one model, since each adds that much again.
 */
class ChatTimeoutInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isChatQuestion = request.method == "POST" && request.url.encodedPath.endsWith(CHAT_MESSAGES_PATH)
        val configured = if (isChatQuestion) chain.withReadTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS) else chain
        return configured.proceed(request)
    }

    private companion object {
        const val CHAT_MESSAGES_PATH = "/tin-nhan"
        const val READ_TIMEOUT_SECONDS = 130
    }
}
