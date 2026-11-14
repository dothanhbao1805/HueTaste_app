package com.example.huetaste.feature.profile

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.clicksComingSoon
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.databinding.FragmentProfileBinding
import com.example.huetaste.databinding.ItemProfileRowBinding
import com.example.huetaste.domain.model.UserProfile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The user's own corner: who is signed in, links to their taste, activity and account, and signing out. */
@AndroidEntryPoint
class ProfileFragment : LsFragment<FragmentProfileBinding>(FragmentProfileBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[ProfileViewModel::class.java] }

    override fun onViewCreated() {
        with(binding) {
            bindRow(rowTaste, R.drawable.ic_restaurant, R.color.hue_primary_fixed, R.color.hue_primary, R.string.profile_taste_title, R.string.profile_taste_subtitle)
            bindRow(rowDiet, R.drawable.ic_eco, R.color.hue_secondary_fixed, R.color.hue_on_secondary_fixed, R.string.profile_diet_title, R.string.profile_diet_subtitle)
            bindRow(rowNeed, R.drawable.ic_tune, R.color.hue_tertiary_fixed, R.color.hue_tertiary, R.string.profile_need_title, R.string.profile_need_subtitle)
            bindRow(rowScanHistory, R.drawable.ic_document_scanner, R.color.hue_surface_container, R.color.hue_on_surface_variant, R.string.profile_scan_history_title, R.string.profile_scan_history_subtitle)
            bindRow(rowReports, R.drawable.ic_flag, R.color.hue_surface_container, R.color.hue_on_surface_variant, R.string.profile_reports_title, R.string.profile_reports_subtitle)
            bindRow(rowPassword, R.drawable.ic_lock, R.color.hue_surface_container, R.color.hue_on_surface_variant, R.string.profile_password_title)
            bindRow(rowLogout, R.drawable.ic_logout, R.color.hue_error_container, R.color.hue_error, R.string.profile_logout_title)
            rowLogout.rowTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.hue_error))
            rowLogout.rowChevron.isVisible = false

            // shortcut: these screens are not built yet; replace each with its screen when it lands.
            listOf(editButton, rowTaste.root, rowDiet.root, rowNeed.root, rowScanHistory.root, rowReports.root, rowPassword.root)
                .forEach { it.clicksComingSoon() }
            rowLogout.root.clicks { LogoutDialog.show(childFragmentManager) }
            guestLoginButton.clicks { requireActivity().startLoginActivity() }
        }

        // MainActivity watches the session and opens the login once it is cleared.
        childFragmentManager.setFragmentResultListener(LogoutDialog.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
            viewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ProfileUiState) = with(binding) {
        val user = (state as? ProfileUiState.SignedIn)?.user
        val isGuest = state == ProfileUiState.Guest
        accountSections.isVisible = user != null
        editButton.isVisible = user != null
        guestLoginButton.isVisible = isGuest
        avatarGuest.isVisible = isGuest
        when {
            user != null -> showUser(user)
            isGuest -> {
                nameText.setText(R.string.profile_guest_name)
                detailText.setText(R.string.profile_guest_message)
            }
        }
    }

    private fun showUser(user: UserProfile) = with(binding) {
        nameText.text = user.name
        detailText.text = user.email
        avatarInitial.text = user.name.trim().firstOrNull()?.uppercase()
        val avatarUrl = user.avatarUrl
        avatarImage.isVisible = avatarUrl != null
        // A photo that fails to load leaves the initial on show.
        if (avatarUrl != null) avatarImage.load(avatarUrl) { listener(onError = { _, _ -> avatarImage.isVisible = false }) }
    }

    private fun bindRow(
        row: ItemProfileRowBinding,
        @DrawableRes icon: Int,
        @ColorRes iconBackground: Int,
        @ColorRes iconTint: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int? = null,
    ) {
        val context = requireContext()
        row.rowIcon.setImageResource(icon)
        row.rowIcon.backgroundTintList = ContextCompat.getColorStateList(context, iconBackground)
        row.rowIcon.imageTintList = ContextCompat.getColorStateList(context, iconTint)
        row.rowTitle.setText(title)
        row.rowSubtitle.isVisible = subtitle != null
        subtitle?.let(row.rowSubtitle::setText)
    }
}
