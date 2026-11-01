package com.example.huetaste.data.remote

import com.example.huetaste.data.remote.dto.DangKyBody
import com.example.huetaste.data.remote.dto.DangNhapBody
import com.example.huetaste.data.remote.dto.RefreshBody
import com.example.huetaste.data.remote.dto.TokenOutDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Sign-in endpoints. They run on a client without the token interceptor and authenticator, so a rejected
 * refresh can never trigger another refresh.
 */
interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: DangNhapBody): TokenOutDto

    @POST("auth/register")
    suspend fun register(@Body body: DangKyBody): TokenOutDto

    /** Blocking on purpose: it is called from OkHttp's authenticator, which already runs off the main thread. */
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshBody): Call<TokenOutDto>
}
