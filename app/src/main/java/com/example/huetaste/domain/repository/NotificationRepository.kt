package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.model.NotificationPage

interface NotificationRepository {
    /** The backend also creates the reminders that are due (meal time, lunar day, weather) on this call. */
    suspend fun getPage(page: Int): NotificationPage

    suspend fun unreadCount(): Int

    suspend fun markRead(id: String)

    suspend fun markAllRead()

    /** Unread notifications that were never shown in the status bar; they count as shown from now on. */
    suspend fun takeUnshown(): List<AppNotification>

    /** True only the first time it is called on this device. */
    suspend fun claimPermissionPrompt(): Boolean
}
