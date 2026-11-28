package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(oldPassword: String, newPassword: String) =
        repository.changePassword(oldPassword, newPassword)
}
