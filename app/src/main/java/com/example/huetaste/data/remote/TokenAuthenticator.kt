package com.example.huetaste.data.remote

import com.example.huetaste.data.local.SecureSessionStore
import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.dto.RefreshBody
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Provider

/**
 * Renews an expired access token. When the backend answers 401 to a request that carried a token, the refresh
 * token is exchanged once and the request is repeated. If the backend rejects the refresh token the session is
 * cleared, which sends the user back to the login. A network failure keeps the session so a later request can
 * try again.
 */
class TokenAuthenticator @Inject constructor(
    private val store: SecureSessionStore,
    // Provider keeps this class out of the dependency cycle between the two HTTP clients.
    private val authApi: Provider<AuthApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val sentHeader = response.request.header(AuthInterceptor.AUTHORIZATION) ?: return null
        if (responseCount(response) >= MAX_ATTEMPTS) return null
        return synchronized(this) { refresh(response.request, sentHeader) }
    }

    private fun refresh(request: Request, sentHeader: String): Request? {
        val current = store.session() ?: return null
        val sentToken = sentHeader.removePrefix("${AuthInterceptor.BEARER} ")
        // Another request refreshed the tokens while this one waited for the lock.
        if (current.accessToken != sentToken) return withToken(request, current.accessToken)

        val result = try {
            authApi.get().refresh(RefreshBody(current.refreshToken)).execute()
        } catch (e: IOException) {
            return null
        }
        val body = result.body()
        return when {
            result.isSuccessful && body != null -> {
                val renewed = body.toDomain()
                store.save(renewed)
                withToken(request, renewed.accessToken)
            }
            result.code() == HttpURLConnection.HTTP_UNAUTHORIZED || result.code() == HttpURLConnection.HTTP_FORBIDDEN -> {
                store.clear()
                null
            }
            else -> null
        }
    }

    private fun withToken(request: Request, token: String): Request =
        request.newBuilder().header(AuthInterceptor.AUTHORIZATION, "${AuthInterceptor.BEARER} $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
