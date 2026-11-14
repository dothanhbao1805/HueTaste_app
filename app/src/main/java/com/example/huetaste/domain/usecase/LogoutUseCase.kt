package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

/** Forgets the session; the main screen sees the signed-out state and goes back to the login. */
class LogoutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
