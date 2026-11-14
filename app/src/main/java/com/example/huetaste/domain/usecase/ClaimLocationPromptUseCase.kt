package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.LocationProvider
import javax.inject.Inject

class ClaimLocationPromptUseCase @Inject constructor(private val provider: LocationProvider) {
    suspend operator fun invoke(): Boolean = provider.claimPermissionPrompt()
}
