package com.example.huetaste.feature.ai_chat

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.startAiChatActivity
import com.example.huetaste.databinding.ActivityChatHistoryBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.ChatConversation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Stitch "Trợ Lý AI - Lịch Sử Trò Chuyện" screen backed by `GET /tro-chuyen`. Opening a conversation, or
 * starting a new one, returns to the chat screen that opened this one.
 */
@AndroidEntryPoint
class ChatHistoryActivity : LsActivity<ActivityChatHistoryBinding>(ActivityChatHistoryBinding::inflate) {
    private val viewModel: ChatHistoryViewModel by viewModels()

    private val conversationAdapter = ConversationAdapter(
        onOpen = { row -> openChat(row.id) },
        onDelete = { row -> DeleteConversationDialog.show(supportFragmentManager, row.id) },
    )

    private val weekday = SimpleDateFormat("EEEE", Locale("vi", "VN"))

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.conversationList.adapter = conversationAdapter
        binding.conversationList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layout.findLastVisibleItemPosition() >= conversationAdapter.itemCount - LOAD_MORE_AHEAD) {
                    viewModel.loadMore()
                }
            }
        })

        binding.backButton.clicks { handleBackPressed() }
        binding.newChatButton.clicks { openChat(null) }
        binding.startChatButton.clicks { openChat(null) }
        binding.retryButton.clicks { viewModel.reload() }
        supportFragmentManager.setFragmentResultListener(DeleteConversationDialog.REQUEST_KEY, this) { _, result ->
            result.getString(DeleteConversationDialog.RESULT_ID)?.let(viewModel::delete)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            ChatHistoryEvent.DeleteFailed -> makeToast(R.string.ai_history_delete_failed)
                            ChatHistoryEvent.LoadMoreFailed -> makeToast(R.string.ai_chat_error_network)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ChatHistoryUiState) {
        val items = buildItems(state)
        conversationAdapter.submitList(items)
        val error = state.error
        binding.loadingProgress.isVisible = state.isLoading
        binding.errorView.isVisible = !state.isLoading && error != null
        binding.errorText.text = error?.let(::errorMessage)
        val showList = !state.isLoading && error == null
        binding.conversationList.isVisible = showList && state.conversations.isNotEmpty()
        binding.emptyView.isVisible = showList && state.conversations.isEmpty()
    }

    /** Groups the conversations by "Hôm nay", "Hôm qua", "Tuần trước" (the 7 days before) or their date. */
    private fun buildItems(state: ChatHistoryUiState): List<ConversationUiItem> {
        val groups = state.conversations.groupBy { groupLabel(it) }
        val items = groups.flatMap { (label, group) ->
            listOf<ConversationUiItem>(
                ConversationUiItem.Header(label, getString(R.string.ai_history_count, group.size)),
            ) + group.map { conversation ->
                ConversationUiItem.Row(
                    id = conversation.id,
                    title = conversation.title ?: getString(R.string.ai_history_untitled),
                    time = rowTime(conversation),
                )
            }
        }
        return if (state.isLoadingMore) items + ConversationUiItem.LoadingMore else items
    }

    private fun groupLabel(conversation: ChatConversation): String {
        val dayStart = ChatItemsBuilder.dayStart(conversation.createdAt)
        return when (ChatFormat.daysAgo(dayStart)) {
            0 -> getString(R.string.ai_chat_today)
            1 -> getString(R.string.ai_chat_yesterday)
            in 2..7 -> getString(R.string.ai_chat_last_week)
            else -> ChatFormat.date(dayStart)
        }
    }

    private fun rowTime(conversation: ChatConversation): String {
        val dayStart = ChatItemsBuilder.dayStart(conversation.createdAt)
        return when (ChatFormat.daysAgo(dayStart)) {
            0, 1 -> ChatFormat.time(conversation.createdAt)
            in 2..7 -> weekday.format(Date(conversation.createdAt)).replaceFirstChar { it.uppercase() }
            else -> ChatFormat.time(conversation.createdAt)
        }
    }

    private fun errorMessage(error: AppError): String = when (error.code) {
        AppError.NETWORK -> getString(R.string.ai_chat_error_network)
        AppError.UNAUTHORIZED, AppError.INVALID_TOKEN, AppError.TOKEN_EXPIRED -> getString(R.string.ai_chat_sign_in_message)
        else -> getString(R.string.ai_history_load_failed)
    }

    private fun openChat(conversationId: String?) {
        startAiChatActivity(conversationId)
        finish()
    }

    companion object {
        /** Rows left below the last visible one when the next page starts loading. */
        private const val LOAD_MORE_AHEAD = 4

        fun newIntent(context: Context): Intent = Intent(context, ChatHistoryActivity::class.java)
    }
}
