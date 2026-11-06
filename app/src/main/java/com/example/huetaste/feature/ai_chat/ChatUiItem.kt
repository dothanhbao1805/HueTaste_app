package com.example.huetaste.feature.ai_chat

import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.Place

/**
 * One row of the assistant chat, built from the backend `/tro-chuyen` messages and replies. Times are epoch
 * milliseconds; the adapter formats them, and the error text, for display.
 */
sealed interface ChatUiItem {
    /** Stable identity for list diffing. */
    val key: String

    /** [dayStart] is local midnight of the day the following messages were written. */
    data class DateHeader(val dayStart: Long) : ChatUiItem {
        override val key = "date_$dayStart"
    }

    /** The assistant's greeting and the quick questions; written by the app, not stored by the backend. */
    object Welcome : ChatUiItem {
        override val key = "welcome"
    }

    /** [pending] while the question is on its way and not yet stored by the backend. */
    data class UserMessage(val id: String, val text: String, val time: Long, val pending: Boolean = false) : ChatUiItem {
        override val key = "user_$id"
    }

    data class AssistantMessage(val id: String, val text: String, val time: Long) : ChatUiItem {
        override val key = "assistant_$id"
    }

    /** The dish suggestions of one reply, shown as one sideways row. */
    data class DishCards(val id: String, val cards: List<ChatDishCard>) : ChatUiItem {
        override val key = "dishes_$id"
    }

    data class PlaceCard(
        val id: String,
        val place: Place,
        val distanceKm: Double?,
        val reason: String?,
    ) : ChatUiItem {
        override val key = "place_${id}_${place.id}"
    }

    /** The backend takes about 3-8 seconds to answer. */
    object Typing : ChatUiItem {
        override val key = "typing"
    }

    /** A failed question or a conversation that could not be loaded; [code] and [message] come from the backend. */
    data class Error(val id: String, val code: String?, val message: String?, val canRetry: Boolean) : ChatUiItem {
        override val key = "error_$id"
    }
}

/** [matchPercent] is the backend score (0..1) as a percentage. */
data class ChatDishCard(
    val dish: Dish,
    val matchPercent: Int?,
    val reason: String?,
)
