package com.example.huetaste.data.repository

import com.example.huetaste.BuildConfig
import com.example.huetaste.data.local.SecureSessionStore
import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.AuthApi
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.CapNhatThongTinBody
import com.example.huetaste.data.remote.dto.DangKyBody
import com.example.huetaste.data.remote.dto.DangNhapBody
import com.example.huetaste.data.remote.dto.DatLaiMatKhauBody
import com.example.huetaste.data.remote.dto.DoiMatKhauBody
import com.example.huetaste.data.remote.dto.QuenMatKhauBody
import com.example.huetaste.data.remote.dto.XacThucOtpBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    /** The client that carries the access token, for calls on the signed-in account. */
    private val accountApi: HueTasteApi,
    private val store: SecureSessionStore,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthSession = withContext(ioDispatcher) {
        val session = apiCall(gson) { api.login(DangNhapBody(email.trim(), password)).toDomain() }
        store.save(session)
        session
    }

    override suspend fun register(name: String, email: String, password: String): AuthSession =
        withContext(ioDispatcher) {
            val session = apiCall(gson) { api.register(DangKyBody(email.trim(), password, name.trim())).toDomain() }
            store.save(session)
            session
        }

    override suspend fun requestPasswordReset(email: String) = withContext(ioDispatcher) {
        apiCall(gson) { api.forgotPassword(QuenMatKhauBody(email.trim())) }
    }

    override suspend fun verifyResetCode(email: String, code: String) = withContext(ioDispatcher) {
        apiCall(gson) { api.verifyOtp(XacThucOtpBody(email.trim(), code)) }
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String) = withContext(ioDispatcher) {
        apiCall(gson) { api.resetPassword(DatLaiMatKhauBody(email.trim(), code, newPassword)) }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) = withContext(ioDispatcher) {
        apiCall(gson) { accountApi.changePassword(DoiMatKhauBody(oldPassword, newPassword)) }
    }

    override suspend fun continueAsGuest() {
        withContext(ioDispatcher) { store.markGuest() }
    }

    // Reading the store decrypts with the Keystore, so it stays off the main thread.
    override suspend fun currentState(): AuthState = withContext(ioDispatcher) { store.state() }

    override suspend fun currentUser(): UserProfile? = withContext(ioDispatcher) {
        store.session()?.user?.let { user -> user.copy(avatarUrl = user.avatarUrl?.let(::absoluteUrl)) }
    }

    override fun observeState(): Flow<AuthState> = store.changes

    override fun observeUser(): Flow<UserProfile?> = store.changes
        // The value is not used: it only triggers the first read, once changes are already being listened to.
        .onSubscription { emit(AuthState.SIGNED_IN) }
        .map { currentUser() }
        .distinctUntilChanged()

    override suspend fun updateName(name: String) = withContext(ioDispatcher) {
        val user = apiCall(gson) { accountApi.updateProfile(CapNhatThongTinBody(name.trim())).toDomain() }
        store.updateUser(user)
    }

    override suspend fun uploadAvatar(image: File) = withContext(ioDispatcher) {
        val part = MultipartBody.Part.createFormData("file", image.name, image.asRequestBody("image/jpeg".toMediaType()))
        val user = apiCall(gson) { accountApi.uploadAvatar(part).toDomain() }
        store.updateUser(user)
    }

    override suspend fun logout() {
        withContext(ioDispatcher) { store.clear() }
    }

    /** An uploaded avatar is stored as `/uploads/...` on the backend host; a link to another site stays as it is. */
    private fun absoluteUrl(url: String): String = BuildConfig.API_BASE_URL.toHttpUrl().resolve(url)?.toString() ?: url
}
