package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.GuiTinNhanBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.ChatConversation
import com.example.huetaste.domain.model.ChatMessage
import com.example.huetaste.domain.model.ChatReply
import com.example.huetaste.domain.model.ConversationPage
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatRepository {

    override suspend fun createConversation(): ChatConversation = withContext(ioDispatcher) {
        apiCall(gson) { api.createConversation().toDomain() }
    }

    override suspend fun getConversations(page: Int): ConversationPage = withContext(ioDispatcher) {
        apiCall(gson) {
            api.getConversations(page, ConversationPage.PAGE_SIZE).toDomain(page, ConversationPage.PAGE_SIZE)
        }
    }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> = withContext(ioDispatcher) {
        apiCall(gson) {
            // The backend pages messages oldest first; a conversation is short, so read every page.
            val messages = mutableListOf<ChatMessage>()
            var page = 1
            while (page <= MAX_MESSAGE_PAGES) {
                val result = api.getMessages(conversationId, page, MESSAGE_PAGE_SIZE)
                val items = result.items.orEmpty()
                messages += items.map { it.toDomain() }
                if (items.size < MESSAGE_PAGE_SIZE || messages.size >= (result.total ?: 0)) break
                page++
            }
            messages
        }
    }

    override suspend fun sendMessage(conversationId: String, text: String, location: GeoPoint?): ChatReply =
        withContext(ioDispatcher) {
            apiCall(gson) {
                api.sendMessage(
                    conversationId,
                    GuiTinNhanBody(noiDung = text, viDo = location?.latitude, kinhDo = location?.longitude),
                ).toDomain()
            }
        }

    override suspend fun deleteConversation(conversationId: String) {
        withContext(ioDispatcher) {
            apiCall(gson) { api.deleteConversation(conversationId) }
        }
    }

    private companion object {
        /** The backend's largest page size for messages. */
        const val MESSAGE_PAGE_SIZE = 100

        /** Guards against an endless loop if the backend kept returning full pages. */
        const val MAX_MESSAGE_PAGES = 20
    }
}
