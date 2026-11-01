package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAuthStateUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): AuthState = repository.currentState()

    /** Emits again whenever the session changes. */
    fun observe(): Flow<AuthState> = repository.observeState()
}
