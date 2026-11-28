package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.NotificationPage
import com.example.huetaste.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(page: Int): NotificationPage = repository.getPage(page)
}
