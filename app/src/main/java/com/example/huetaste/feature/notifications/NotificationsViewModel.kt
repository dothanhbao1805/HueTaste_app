package com.example.huetaste.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.NotificationType
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetNotificationsUseCase
import com.example.huetaste.domain.usecase.MarkAllNotificationsReadUseCase
import com.example.huetaste.domain.usecase.MarkNotificationReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    /** The first page failed; the screen shows the error with "Thử lại". */
    val error: AppError? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    /** Notifications belong to an account, so a guest is asked to sign in. */
    val requiresSignIn: Boolean = false,
) {
    val hasUnread get() = items.any { !it.isRead }
}

sealed interface NotificationsEvent {
    /** Go where the notification is about. */
    data class Open(val type: NotificationType, val dishId: String?) : NotificationsEvent
    object LoadMoreFailed : NotificationsEvent
    object MarkAllFailed : NotificationsEvent
}

/** Pages through `GET /users/me/thong-bao` (newest first) and marks notifications read. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getAuthState: GetAuthStateUseCase,
    private val getNotifications: GetNotificationsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _events = Channel<NotificationsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            if (getAuthState() == AuthState.SIGNED_IN) reload()
            else _uiState.value = NotificationsUiState(isLoading = false, requiresSignIn = true)
        }
    }

    fun reload() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null, isLoadingMore = false) }
        loadJob = viewModelScope.launch {
            try {
                val page = getNotifications(1)
                nextPage = 2
                _uiState.value = NotificationsUiState(items = page.items, isLoading = false, hasMore = page.hasMore)
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    /** Called when the list nears its end. */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.error != null) return
        _uiState.update { it.copy(isLoadingMore = true) }
        loadJob = viewModelScope.launch {
            try {
                val page = getNotifications(nextPage)
                nextPage++
                _uiState.update { current ->
                    val known = current.items.map { it.id }.toSet()
                    // A notification created meanwhile shifts the pages; skip the ones already shown.
                    current.copy(
                        items = current.items + page.items.filterNot { it.id in known },
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _events.send(NotificationsEvent.LoadMoreFailed)
            }
        }
    }

    fun open(notification: AppNotification) {
        if (!notification.isRead) markRead(notification.id)
        _events.trySend(NotificationsEvent.Open(notification.type, notification.dishId))
    }

    /** The list shows it read at once; a failed call only means it shows unread again next time. */
    fun markRead(id: String) {
        _uiState.update { state -> state.copy(items = state.items.map { if (it.id == id) it.copy(isRead = true) else it }) }
        viewModelScope.launch {
            try {
                markNotificationRead(id)
            } catch (e: AppError) {
                // Nothing to undo on screen: the next load shows the backend's state.
            }
        }
    }

    fun markAllRead() {
        if (!_uiState.value.hasUnread) return
        _uiState.update { state -> state.copy(items = state.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch {
            try {
                markAllNotificationsRead()
            } catch (e: AppError) {
                _events.send(NotificationsEvent.MarkAllFailed)
                reload()
            }
        }
    }
}
