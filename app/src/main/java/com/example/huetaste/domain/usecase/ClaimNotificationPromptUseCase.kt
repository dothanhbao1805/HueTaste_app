package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.NotificationRepository
import javax.inject.Inject

class ClaimNotificationPromptUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(): Boolean = repository.claimPermissionPrompt()
}
