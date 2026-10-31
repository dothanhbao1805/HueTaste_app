package com.example.huetaste.feature.scan.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsDialog
import com.base.common.extension.clicks
import com.example.huetaste.databinding.DialogScanMessageBinding

/**
 * Two-button message used by the scan flow (camera permission, identification errors).
 *
 * The answer goes back through the fragment result API under the request key it was shown with,
 * so it still reaches the Activity after recreation.
 */
class ScanMessageDialog : LsDialog<DialogScanMessageBinding>(DialogScanMessageBinding::inflate) {

    override fun isFull(): Boolean = false

    override fun onViewCreated() {
        // Both buttons lead somewhere, so the dialog cannot be dismissed without an answer.
        isCancelable = false
        val args = requireArguments()
        binding.titleText.text = args.getString(ARG_TITLE)
        binding.messageText.text = args.getString(ARG_MESSAGE)
        binding.confirmButton.text = args.getString(ARG_CONFIRM)
        binding.cancelButton.text = args.getString(ARG_CANCEL)
        binding.confirmButton.clicks { answer(confirmed = true) }
        binding.cancelButton.clicks { answer(confirmed = false) }
    }

    private fun answer(confirmed: Boolean) {
        val requestKey = requireArguments().getString(ARG_REQUEST_KEY) ?: return
        parentFragmentManager.setFragmentResult(requestKey, bundleOf(RESULT_CONFIRMED to confirmed))
        dismiss()
    }

    companion object {
        const val RESULT_CONFIRMED = "confirmed"
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CONFIRM = "confirm"
        private const val ARG_CANCEL = "cancel"

        fun show(
            fragmentManager: FragmentManager,
            requestKey: String,
            title: String,
            message: String,
            confirm: String,
            cancel: String,
        ) {
            // A dialog restored after recreation already answers this request.
            if (fragmentManager.findFragmentByTag(requestKey) != null) return
            ScanMessageDialog().apply {
                arguments = bundleOf(
                    ARG_REQUEST_KEY to requestKey,
                    ARG_TITLE to title,
                    ARG_MESSAGE to message,
                    ARG_CONFIRM to confirm,
                    ARG_CANCEL to cancel,
                )
            }.show(fragmentManager, requestKey)
        }
    }
}
