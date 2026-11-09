package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): UserProfile? = repository.currentUser()
}
