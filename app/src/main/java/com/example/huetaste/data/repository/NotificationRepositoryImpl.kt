package com.example.huetaste.data.repository

import com.example.huetaste.data.local.ShownNotificationStore
import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.model.NotificationPage
import com.example.huetaste.domain.repository.NotificationRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val shownStore: ShownNotificationStore,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {

    override suspend fun getPage(page: Int): NotificationPage = withContext(ioDispatcher) {
        apiCall(gson) { api.getNotifications(page, NotificationPage.PAGE_SIZE).toDomain() }
    }

    override suspend fun unreadCount(): Int = withContext(ioDispatcher) {
        apiCall(gson) { api.getUnreadNotificationCount().soChuaDoc ?: 0 }
    }

    override suspend fun markRead(id: String) = withContext(ioDispatcher) {
        apiCall(gson) { api.markNotificationRead(id) }
    }

    override suspend fun markAllRead() = withContext(ioDispatcher) {
        apiCall(gson) { api.markAllNotificationsRead() }
    }

    override suspend fun takeUnshown(): List<AppNotification> = withContext(ioDispatcher) {
        val unread = getPage(1).items.filterNot { it.isRead }
        val fresh = shownStore.claimNew(unread.map { it.id })
        unread.filter { it.id in fresh }
    }

    override suspend fun claimPermissionPrompt(): Boolean = withContext(ioDispatcher) { shownStore.claimPermissionPrompt() }
}
