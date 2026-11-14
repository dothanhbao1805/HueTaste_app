package com.example.huetaste.feature.profile

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsDialog
import com.base.common.extension.clicks
import com.example.huetaste.databinding.DialogLogoutBinding

/**
 * Asks before signing out. A confirmed answer goes back through the fragment result API under [REQUEST_KEY], so it
 * still reaches the profile tab after recreation.
 */
class LogoutDialog : LsDialog<DialogLogoutBinding>(DialogLogoutBinding::inflate) {

    override fun isFull(): Boolean = false

    override fun onViewCreated() {
        binding.cancelButton.clicks { dismiss() }
        binding.confirmButton.clicks {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf())
            dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY = "logout"

        fun show(fragmentManager: FragmentManager) {
            // A dialog restored after recreation already asks.
            if (fragmentManager.findFragmentByTag(REQUEST_KEY) != null) return
            LogoutDialog().show(fragmentManager, REQUEST_KEY)
        }
    }
}
