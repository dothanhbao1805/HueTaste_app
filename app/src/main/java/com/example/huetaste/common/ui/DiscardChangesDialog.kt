package com.example.huetaste.common.ui

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsDialog
import com.base.common.extension.clicks
import com.example.huetaste.databinding.DialogDiscardChangesBinding

/**
 * Asks before leaving a screen with unsaved edits. Discarding goes back through the fragment result API under
 * [REQUEST_KEY], so it still reaches the screen after recreation.
 */
class DiscardChangesDialog : LsDialog<DialogDiscardChangesBinding>(DialogDiscardChangesBinding::inflate) {

    override fun isFull(): Boolean = false

    override fun onViewCreated() {
        binding.stayButton.clicks { dismiss() }
        binding.discardButton.clicks {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf())
            dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY = "discard_changes"

        fun show(fragmentManager: FragmentManager) {
            // A dialog restored after recreation already asks.
            if (fragmentManager.findFragmentByTag(REQUEST_KEY) != null) return
            DiscardChangesDialog().show(fragmentManager, REQUEST_KEY)
        }
    }
}
