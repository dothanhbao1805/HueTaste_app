package com.example.huetaste.feature.ai_chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.ChatConversation
import com.example.huetaste.domain.usecase.DeleteConversationUseCase
import com.example.huetaste.domain.usecase.GetConversationsUseCase
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

data class ChatHistoryUiState(
    val conversations: List<ChatConversation> = emptyList(),
    val isLoading: Boolean = true,
    /** The first page failed; the screen shows the error with "Thử lại". */
    val error: AppError? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

sealed interface ChatHistoryEvent {
    object DeleteFailed : ChatHistoryEvent
    object LoadMoreFailed : ChatHistoryEvent
}

/** Pages through `GET /tro-chuyen` (newest first) and deletes conversations. */
@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val getConversations: GetConversationsUseCase,
    private val deleteConversation: DeleteConversationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHistoryUiState())
    val uiState: StateFlow<ChatHistoryUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChatHistoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null, isLoadingMore = false) }
        loadJob = viewModelScope.launch {
            try {
                val page = getConversations(1)
                nextPage = 2
                _uiState.update {
                    ChatHistoryUiState(conversations = page.items, isLoading = false, hasMore = page.hasMore)
                }
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
                val page = getConversations(nextPage)
                nextPage++
                _uiState.update { current ->
                    val known = current.conversations.map { it.id }.toSet()
                    current.copy(
                        // A conversation created meanwhile shifts the pages; skip the ones already shown.
                        conversations = current.conversations + page.items.filterNot { it.id in known },
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _events.send(ChatHistoryEvent.LoadMoreFailed)
            }
        }
    }

    /** Removes the row at once and puts it back if the backend refuses. */
    fun delete(conversationId: String) {
        val before = _uiState.value.conversations
        val index = before.indexOfFirst { it.id == conversationId }
        if (index < 0) return
        val removed = before[index]
        _uiState.update { it.copy(conversations = before - removed) }
        viewModelScope.launch {
            try {
                deleteConversation(conversationId)
            } catch (e: AppError) {
                _uiState.update { current ->
                    if (current.conversations.any { it.id == conversationId }) return@update current
                    val restored = current.conversations.toMutableList()
                    restored.add(index.coerceAtMost(restored.size), removed)
                    current.copy(conversations = restored)
                }
                _events.send(ChatHistoryEvent.DeleteFailed)
            }
        }
    }
}
