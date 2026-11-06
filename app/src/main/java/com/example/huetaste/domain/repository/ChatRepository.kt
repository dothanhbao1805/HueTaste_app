package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.ChatConversation
import com.example.huetaste.domain.model.ChatMessage
import com.example.huetaste.domain.model.ChatReply
import com.example.huetaste.domain.model.ConversationPage
import com.example.huetaste.domain.model.GeoPoint

/** The signed-in user's assistant conversations. */
interface ChatRepository {
    suspend fun createConversation(): ChatConversation

    /** Newest first. */
    suspend fun getConversations(page: Int): ConversationPage

    /** Every message of the conversation, oldest first. */
    suspend fun getMessages(conversationId: String): List<ChatMessage>

    /** Asks the assistant; [location] lets it find places near the user. */
    suspend fun sendMessage(conversationId: String, text: String, location: GeoPoint?): ChatReply

    suspend fun deleteConversation(conversationId: String)
}
