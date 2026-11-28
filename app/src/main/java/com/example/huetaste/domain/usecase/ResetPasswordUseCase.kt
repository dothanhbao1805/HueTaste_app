package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String, newPassword: String) =
        repository.resetPassword(email, code, newPassword)
}
