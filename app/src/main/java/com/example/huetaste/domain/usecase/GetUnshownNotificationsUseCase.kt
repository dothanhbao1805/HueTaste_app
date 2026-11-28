package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.repository.NotificationRepository
import javax.inject.Inject

class GetUnshownNotificationsUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(): List<AppNotification> = repository.takeUnshown()
}
