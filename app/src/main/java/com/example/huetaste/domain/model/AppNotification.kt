package com.example.huetaste.domain.model

/** What a notification is about, which decides its icon and where tapping it leads. */
enum class NotificationType { REPORT, SCANNED_DISH, MEAL_REMINDER, LUNAR_DAY, WEATHER, OTHER }

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val dishId: String?,
    val placeId: String?,
    val reportId: String?,
    val isRead: Boolean,
    /** ISO-8601 local time as sent by the backend. */
    val createdAt: String?,
)

/** One page of notifications, newest first. */
data class NotificationPage(
    val items: List<AppNotification>,
    val total: Int,
    val page: Int,
) {
    val hasMore get() = page * PAGE_SIZE < total

    companion object {
        const val PAGE_SIZE = 20
    }
}
