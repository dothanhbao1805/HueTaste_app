package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Signs in and remembers the session for later launches. */
    suspend fun login(email: String, password: String): AuthSession

    /** Creates the account; the backend signs the user in straight away, so the session is remembered too. */
    suspend fun register(name: String, email: String, password: String): AuthSession

    /** Asks the backend to email a 6-digit reset code; it does not say whether the account exists. */
    suspend fun requestPasswordReset(email: String)

    /** Checks the emailed code; a wrong code counts towards the backend's attempt limit. */
    suspend fun verifyResetCode(email: String, code: String)

    /** Sets a new password with the emailed code; the user then signs in with it. */
    suspend fun resetPassword(email: String, code: String, newPassword: String)

    /** Changes the signed-in user's password; the current session stays valid. */
    suspend fun changePassword(oldPassword: String, newPassword: String)

    /** Remembers that the user chose to use the app without an account. */
    suspend fun continueAsGuest()

    suspend fun currentState(): AuthState

    /** The signed-in user as of the last sign-in, or null for a guest. */
    suspend fun currentUser(): UserProfile?

    /** Emits the new state whenever the session changes, for example when the backend rejects the refresh token. */
    fun observeState(): Flow<AuthState>

    suspend fun logout()
}
