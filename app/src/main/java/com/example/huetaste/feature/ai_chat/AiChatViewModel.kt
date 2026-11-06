package com.example.huetaste.feature.ai_chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetConversationMessagesUseCase
import com.example.huetaste.domain.usecase.GetCurrentLocationUseCase
import com.example.huetaste.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiChatUiState(
    val items: List<ChatUiItem> = emptyList(),
    /** A question is on its way; another one cannot be sent meanwhile. */
    val isSending: Boolean = false,
    /** A stored conversation is loading. */
    val isLoading: Boolean = false,
    /** Guests cannot chat: the backend only answers signed-in users. */
    val requiresSignIn: Boolean = false,
    val usesLocation: Boolean = false,
)

/**
 * Owns the open conversation. A new conversation is only created on the backend when its first question is sent.
 * The conversation id, the location choice and a question that failed survive process death.
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getAuthState: GetAuthStateUseCase,
    private val sendChatMessage: SendChatMessageUseCase,
    private val getConversationMessages: GetConversationMessagesUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState(usesLocation = savedState[KEY_USES_LOCATION] ?: false))
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var conversationId: String?
        get() = savedState[KEY_CONVERSATION_ID]
        set(value) {
            savedState[KEY_CONVERSATION_ID] = value
        }

    /** The question to send again from the error bubble. */
    private var failedQuestion: String?
        get() = savedState[KEY_FAILED_QUESTION]
        set(value) {
            savedState[KEY_FAILED_QUESTION] = value
        }

    private var loadJob: Job? = null
    private var sendJob: Job? = null
    private var lastLoadFailed = false

    init {
        viewModelScope.launch {
            showAuthState(getAuthState())
            getAuthState.observe().collect(::showAuthState)
        }
        // The intent extra only counts the first time; afterwards the saved id is the open conversation.
        val restored = if (savedState.contains(KEY_CONVERSATION_ID)) {
            conversationId
        } else {
            savedState.get<String>(AiChatActivity.EXTRA_CONVERSATION_ID)
        }
        open(restored, force = true)
    }

    /** Shows [id], or a new conversation when it is null. Opening the conversation already shown does nothing. */
    fun open(id: String?, force: Boolean = false) {
        if (!force && id != null && id == conversationId && !lastLoadFailed) return
        loadJob?.cancel()
        sendJob?.cancel()
        conversationId = id
        failedQuestion = null
        lastLoadFailed = false
        _uiState.update { it.copy(isSending = false) }
        if (id == null) {
            _uiState.update { it.copy(items = ChatItemsBuilder.newConversation(), isLoading = false) }
            return
        }
        _uiState.update { it.copy(items = emptyList(), isLoading = true) }
        loadJob = viewModelScope.launch {
            try {
                val messages = getConversationMessages(id)
                val items = if (messages.isEmpty()) {
                    ChatItemsBuilder.newConversation()
                } else {
                    ChatItemsBuilder.fromHistory(messages)
                }
                _uiState.update { it.copy(items = items, isLoading = false) }
            } catch (e: AppError) {
                lastLoadFailed = true
                _uiState.update {
                    it.copy(items = listOf(errorItem("load", e, canRetry = true)), isLoading = false)
                }
            }
        }
    }

    fun newConversation() = open(null)

    /** @return false when nothing was sent (empty text, a guest, or a question already on its way). */
    fun send(rawText: String): Boolean {
        val text = rawText.trim().take(MAX_LENGTH)
        val state = _uiState.value
        if (text.isEmpty() || state.isSending || state.isLoading || state.requiresSignIn) return false
        val pending = ChatUiItem.UserMessage(
            id = "pending_${System.currentTimeMillis()}",
            text = text,
            time = System.currentTimeMillis(),
            pending = true,
        )
        val base = state.items.filterNot { it is ChatUiItem.Error }
        ask(text, base + pending + ChatUiItem.Typing, pending.key)
        return true
    }

    /** Sends the failed question again, or loads the conversation again when that failed. */
    fun retry() {
        if (lastLoadFailed) {
            open(conversationId, force = true)
            return
        }
        val question = failedQuestion ?: return
        if (_uiState.value.isSending) return
        val pendingKey = _uiState.value.items.lastOrNull { it is ChatUiItem.UserMessage && it.pending }?.key
        val base = _uiState.value.items.filterNot { it is ChatUiItem.Error }.map { item ->
            if (item is ChatUiItem.UserMessage && item.key == pendingKey) item.copy(pending = true) else item
        }
        ask(question, base + ChatUiItem.Typing, pendingKey)
    }

    fun setUsesLocation(uses: Boolean) {
        savedState[KEY_USES_LOCATION] = uses
        _uiState.update { it.copy(usesLocation = uses) }
    }

    /**
     * Shows [shownItems] (the question and the typing bubble) while asking. On success the temporary question
     * ([pendingKey]) is replaced by the stored one and the answer with its cards; on failure an error bubble with
     * "Thử lại" replaces the typing bubble.
     */
    private fun ask(question: String, shownItems: List<ChatUiItem>, pendingKey: String?) {
        failedQuestion = null
        _uiState.update { it.copy(items = shownItems, isSending = true) }
        sendJob = viewModelScope.launch {
            val location = if (_uiState.value.usesLocation) getCurrentLocation() else null
            try {
                val sent = sendChatMessage(conversationId, question, location)
                conversationId = sent.conversationId
                _uiState.update { state ->
                    val kept = state.items.filterNot { it == ChatUiItem.Typing || it.key == pendingKey }
                    state.copy(items = kept + ChatItemsBuilder.fromReply(sent.reply), isSending = false)
                }
            } catch (e: AppError) {
                failedQuestion = question
                _uiState.update { state ->
                    val kept = state.items.filterNot { it == ChatUiItem.Typing }.map { item ->
                        if (item is ChatUiItem.UserMessage && item.key == pendingKey) item.copy(pending = false) else item
                    }
                    state.copy(items = kept + errorItem("send", e, canRetry = true), isSending = false)
                }
            }
        }
    }

    private fun showAuthState(state: AuthState) {
        _uiState.update { it.copy(requiresSignIn = state != AuthState.SIGNED_IN) }
    }

    private fun errorItem(id: String, error: AppError, canRetry: Boolean) =
        ChatUiItem.Error(id = id, code = error.code, message = error.message, canRetry = canRetry)

    private companion object {
        /** Differs from the intent extra so the saved id can tell a restored screen from a first start. */
        const val KEY_CONVERSATION_ID = "open_conversation_id"
        const val KEY_USES_LOCATION = "uses_location"
        const val KEY_FAILED_QUESTION = "failed_question"

        /** The backend rejects longer questions. */
        const val MAX_LENGTH = 1000
    }
}
