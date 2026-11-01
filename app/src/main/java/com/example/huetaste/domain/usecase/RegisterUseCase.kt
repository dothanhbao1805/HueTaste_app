package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(name: String, email: String, password: String): AuthSession =
        repository.register(name, email, password)
}
