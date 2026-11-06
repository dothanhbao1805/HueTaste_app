package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ConversationPage
import com.example.huetaste.domain.repository.ChatRepository
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(page: Int): ConversationPage = repository.getConversations(page)
}
