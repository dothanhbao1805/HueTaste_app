package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String) = repository.deleteConversation(conversationId)
}
