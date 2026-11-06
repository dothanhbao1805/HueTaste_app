package com.example.huetaste.domain.model

/** One assistant conversation. [title] is the first question, null until something was asked. */
data class ChatConversation(
    val id: String,
    val title: String?,
    /** Epoch milliseconds. */
    val createdAt: Long,
)

data class ConversationPage(
    val items: List<ChatConversation>,
    val total: Int,
    val page: Int,
    val size: Int,
) {
    val hasMore: Boolean get() = page * size < total

    companion object {
        const val PAGE_SIZE = 20
    }
}

enum class ChatSender { USER, ASSISTANT }

enum class ChatMessageKind {
    /** A question or an answer. */
    TEXT,

    /** A stored recommendation: [ChatMessage.dish] or [ChatMessage.place] is set and the text is the reason. */
    SUGGESTION,
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val kind: ChatMessageKind,
    val text: String,
    /** Epoch milliseconds. */
    val createdAt: Long,
    /** How well a suggestion fits the user, from 0 to 1. */
    val score: Double?,
    val dish: Dish?,
    val place: Place?,
)

data class ChatDishSuggestion(
    val dish: Dish,
    val score: Double?,
    val reason: String?,
)

data class ChatPlaceSuggestion(
    val place: Place,
    val distanceKm: Double?,
    val score: Double?,
    val reason: String?,
)

/** The answer to one question: the stored question, the answer and the cards found for it. */
data class ChatReply(
    val userMessage: ChatMessage,
    val reply: ChatMessage,
    val dishes: List<ChatDishSuggestion>,
    val places: List<ChatPlaceSuggestion>,
)

/** [conversationId] may differ from the one asked for when that conversation had to be created again. */
data class SentChatMessage(
    val conversationId: String,
    val reply: ChatReply,
)

data class GeoPoint(val latitude: Double, val longitude: Double)
