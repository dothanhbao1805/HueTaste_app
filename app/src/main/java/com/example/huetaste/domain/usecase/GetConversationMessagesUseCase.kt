package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ChatMessage
import com.example.huetaste.domain.repository.ChatRepository
import javax.inject.Inject

class GetConversationMessagesUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String): List<ChatMessage> = repository.getMessages(conversationId)
}
