package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Signs in and remembers the session for later launches. */
    suspend fun login(email: String, password: String): AuthSession

    /** Creates the account; the backend signs the user in straight away, so the session is remembered too. */
    suspend fun register(name: String, email: String, password: String): AuthSession

    /** Remembers that the user chose to use the app without an account. */
    suspend fun continueAsGuest()

    suspend fun currentState(): AuthState

    /** Emits the new state whenever the session changes, for example when the backend rejects the refresh token. */
    fun observeState(): Flow<AuthState>

    suspend fun logout()
}
