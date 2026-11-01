package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class ContinueAsGuestUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.continueAsGuest()
}
