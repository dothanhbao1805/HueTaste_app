package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.SentChatMessage
import com.example.huetaste.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Sends a question. A conversation is only created on the backend when its first question is sent; one that was
 * deleted meanwhile (from the history screen) is replaced by a new one and the question is sent once more.
 */
class SendChatMessageUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String?, text: String, location: GeoPoint?): SentChatMessage {
        val id = conversationId ?: repository.createConversation().id
        return try {
            SentChatMessage(id, repository.sendMessage(id, text, location))
        } catch (e: AppError) {
            if (conversationId == null || e.code != AppError.CONVERSATION_NOT_FOUND) throw e
            val newId = repository.createConversation().id
            SentChatMessage(newId, repository.sendMessage(newId, text, location))
        }
    }
}
