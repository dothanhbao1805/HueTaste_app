package com.example.huetaste.data.repository

import com.example.huetaste.data.local.SecureSessionStore
import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.AuthApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.DangKyBody
import com.example.huetaste.data.remote.dto.DangNhapBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
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

    override suspend fun continueAsGuest() {
        withContext(ioDispatcher) { store.markGuest() }
    }

    // Reading the store decrypts with the Keystore, so it stays off the main thread.
    override suspend fun currentState(): AuthState = withContext(ioDispatcher) { store.state() }

    override fun observeState(): Flow<AuthState> = store.changes

    override suspend fun logout() {
        withContext(ioDispatcher) { store.clear() }
    }
}
