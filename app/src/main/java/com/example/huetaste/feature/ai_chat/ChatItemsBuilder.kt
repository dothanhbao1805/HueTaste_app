package com.example.huetaste.feature.ai_chat

import com.example.huetaste.domain.model.ChatMessage
import com.example.huetaste.domain.model.ChatMessageKind
import com.example.huetaste.domain.model.ChatReply
import com.example.huetaste.domain.model.ChatSender
import java.util.Calendar
import kotlin.math.roundToInt

/** Turns backend messages and replies into chat rows. */
internal object ChatItemsBuilder {

    /** A conversation with nothing asked yet: today's header and the greeting. */
    fun newConversation(now: Long = System.currentTimeMillis()): List<ChatUiItem> =
        listOf(ChatUiItem.DateHeader(dayStart(now)), ChatUiItem.Welcome)

    /**
     * A stored conversation, oldest first. Consecutive dish suggestions become one sideways row and every place
     * suggestion its own card; a header is added whenever the day changes.
     */
    fun fromHistory(messages: List<ChatMessage>): List<ChatUiItem> {
        val items = mutableListOf<ChatUiItem>()
        var currentDay: Long? = null
        val dishGroup = mutableListOf<ChatDishCard>()
        var dishGroupId: String? = null

        fun flushDishes() {
            val id = dishGroupId ?: return
            items += ChatUiItem.DishCards(id, dishGroup.toList())
            dishGroup.clear()
            dishGroupId = null
        }

        messages.forEach { message ->
            val day = dayStart(message.createdAt)
            if (day != currentDay) {
                flushDishes()
                items += ChatUiItem.DateHeader(day)
                currentDay = day
            }
            val dish = message.dish
            val place = message.place
            when {
                message.kind == ChatMessageKind.SUGGESTION && dish != null -> {
                    if (dishGroupId == null) dishGroupId = message.id
                    dishGroup += ChatDishCard(dish, percent(message.score), message.text.ifBlank { null })
                }
                message.kind == ChatMessageKind.SUGGESTION && place != null -> {
                    flushDishes()
                    items += ChatUiItem.PlaceCard(message.id, place, distanceKm = null, reason = message.text.ifBlank { null })
                }
                message.kind == ChatMessageKind.SUGGESTION -> Unit
                else -> {
                    flushDishes()
                    items += textItem(message)
                }
            }
        }
        flushDishes()
        return items
    }

    /** The rows of one answered question: the stored question, the answer and its cards. */
    fun fromReply(reply: ChatReply): List<ChatUiItem> = buildList {
        add(textItem(reply.userMessage))
        add(textItem(reply.reply))
        if (reply.dishes.isNotEmpty()) {
            add(
                ChatUiItem.DishCards(
                    reply.reply.id,
                    reply.dishes.map { ChatDishCard(it.dish, percent(it.score), it.reason) },
                ),
            )
        }
        reply.places.forEach { suggestion ->
            add(ChatUiItem.PlaceCard(reply.reply.id, suggestion.place, suggestion.distanceKm, suggestion.reason))
        }
    }

    private fun textItem(message: ChatMessage): ChatUiItem = if (message.sender == ChatSender.USER) {
        ChatUiItem.UserMessage(message.id, message.text, message.createdAt)
    } else {
        ChatUiItem.AssistantMessage(message.id, message.text, message.createdAt)
    }

    private fun percent(score: Double?): Int? = score?.let { (it * 100).roundToInt().coerceIn(0, 100) }

    fun dayStart(time: Long): Long = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
