package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthSession = repository.login(email, password)
}
