package com.example.huetaste.data.remote

import com.example.huetaste.data.remote.dto.DangKyBody
import com.example.huetaste.data.remote.dto.DangNhapBody
import com.example.huetaste.data.remote.dto.DatLaiMatKhauBody
import com.example.huetaste.data.remote.dto.QuenMatKhauBody
import com.example.huetaste.data.remote.dto.RefreshBody
import com.example.huetaste.data.remote.dto.TokenOutDto
import com.example.huetaste.data.remote.dto.XacThucOtpBody
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

    /** Always succeeds for a well-formed email, so it never tells whether an account exists. */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: QuenMatKhauBody)

    /** Checks the emailed code without using it up, so a wrong code is reported before the new password. */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: XacThucOtpBody)

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: DatLaiMatKhauBody)

    /** Blocking on purpose: it is called from OkHttp's authenticator, which already runs off the main thread. */
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshBody): Call<TokenOutDto>
}
