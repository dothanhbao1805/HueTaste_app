package com.example.huetaste.feature.ai_chat

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsDialog
import com.base.common.extension.clicks
import com.example.huetaste.databinding.DialogDeleteConversationBinding

/**
 * Asks before a conversation is deleted. A confirmed answer goes back through the fragment result API under
 * [REQUEST_KEY] with the conversation id, so it still reaches the Activity after recreation.
 */
class DeleteConversationDialog :
    LsDialog<DialogDeleteConversationBinding>(DialogDeleteConversationBinding::inflate) {

    override fun isFull(): Boolean = false

    override fun onViewCreated() {
        binding.cancelButton.clicks { dismiss() }
        binding.confirmButton.clicks {
            val id = requireArguments().getString(ARG_ID) ?: return@clicks dismiss()
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ID to id))
            dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY = "delete_conversation"
        const val RESULT_ID = "conversation_id"
        private const val ARG_ID = "id"

        fun show(fragmentManager: FragmentManager, conversationId: String) {
            // A dialog restored after recreation already asks.
            if (fragmentManager.findFragmentByTag(REQUEST_KEY) != null) return
            DeleteConversationDialog().apply {
                arguments = bundleOf(ARG_ID to conversationId)
            }.show(fragmentManager, REQUEST_KEY)
        }
    }
}
