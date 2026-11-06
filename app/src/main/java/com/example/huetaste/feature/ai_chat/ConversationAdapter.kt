package com.example.huetaste.feature.ai_chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.base.common.base.LsMultiListAdapter
import com.base.common.base.LsViewHolder
import com.base.common.extension.clicks
import com.example.huetaste.databinding.ItemConversationBinding
import com.example.huetaste.databinding.ItemConversationHeaderBinding
import com.example.huetaste.databinding.ItemConversationLoadingBinding

/** One row of the conversation history: a day group header or one conversation (`CuocTroChuyenOut`). */
sealed interface ConversationUiItem {
    val key: String

    data class Header(val label: String, val count: String) : ConversationUiItem {
        override val key = "header_$label"
    }

    data class Row(val id: String, val title: String, val time: String) : ConversationUiItem {
        override val key = "row_$id"
    }

    /** The next page is loading. */
    object LoadingMore : ConversationUiItem {
        override val key = "loading_more"
    }
}

class ConversationAdapter(
    private val onOpen: (ConversationUiItem.Row) -> Unit,
    private val onDelete: (ConversationUiItem.Row) -> Unit,
) : LsMultiListAdapter<ConversationUiItem, ViewBinding>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ConversationUiItem.Header -> TYPE_HEADER
        is ConversationUiItem.Row -> TYPE_ROW
        ConversationUiItem.LoadingMore -> TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LsViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewBinding = when (viewType) {
            TYPE_HEADER -> ItemConversationHeaderBinding.inflate(inflater, parent, false)
            TYPE_LOADING -> ItemConversationLoadingBinding.inflate(inflater, parent, false)
            else -> ItemConversationBinding.inflate(inflater, parent, false)
        }
        return LsViewHolder(binding)
    }

    override fun bindItem(item: ConversationUiItem, binding: ViewBinding, position: Int) {
        when (item) {
            is ConversationUiItem.Header -> with(binding as ItemConversationHeaderBinding) {
                headerLabel.text = item.label
                headerCount.text = item.count
            }
            is ConversationUiItem.Row -> with(binding as ItemConversationBinding) {
                conversationTitle.text = item.title
                conversationTime.text = item.time
                conversationRow.clicks { onOpen(item) }
                deleteButton.clicks { onDelete(item) }
            }
            ConversationUiItem.LoadingMore -> Unit
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ConversationUiItem>() {
        override fun areItemsTheSame(oldItem: ConversationUiItem, newItem: ConversationUiItem) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: ConversationUiItem, newItem: ConversationUiItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ROW = 1
        const val TYPE_LOADING = 2
    }
}
