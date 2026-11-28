package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.NotificationRepository
import javax.inject.Inject

class GetUnreadNotificationCountUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(): Int = repository.unreadCount()
}
