package com.example.huetaste.feature.scan_history

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsDialog
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.DialogDeleteScanBinding

/**
 * Asks before a scan is removed from the history. A confirmed answer goes back through the fragment result API under
 * [REQUEST_KEY] with the scan id, so it still reaches the Activity after recreation.
 */
class DeleteScanDialog : LsDialog<DialogDeleteScanBinding>(DialogDeleteScanBinding::inflate) {

    override fun isFull(): Boolean = false

    override fun onViewCreated() {
        binding.deleteMessage.text = getString(R.string.scan_history_delete_message, requireArguments().getString(ARG_NAME))
        binding.cancelButton.clicks { dismiss() }
        binding.confirmButton.clicks {
            val id = requireArguments().getString(ARG_ID) ?: return@clicks dismiss()
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ID to id))
            dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY = "delete_scan"
        const val RESULT_ID = "scan_id"
        private const val ARG_ID = "id"
        private const val ARG_NAME = "name"

        fun show(fragmentManager: FragmentManager, scanId: String, dishName: String) {
            // A dialog restored after recreation already asks.
            if (fragmentManager.findFragmentByTag(REQUEST_KEY) != null) return
            DeleteScanDialog().apply {
                arguments = bundleOf(ARG_ID to scanId, ARG_NAME to dishName)
            }.show(fragmentManager, REQUEST_KEY)
        }
    }
}
