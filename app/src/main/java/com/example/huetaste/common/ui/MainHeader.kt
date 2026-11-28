package com.example.huetaste.common.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.extension.clicks
import com.example.huetaste.domain.model.UserProfile
import kotlinx.coroutines.launch
import com.example.huetaste.R
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ViewMainHeaderBinding
import com.example.huetaste.domain.model.DeviceArea

/** Shows [area] in the header; [subtitle] names the tab under a known area name. null means still locating. */
fun ViewMainHeaderBinding.showArea(area: DeviceArea?, @StringRes subtitle: Int) {
    when (area) {
        null -> {
            locationTitle.setText(R.string.home_location_loading)
            locationSubtitle.setText(subtitle)
        }
        DeviceArea.PermissionMissing, DeviceArea.LocationOff -> {
            locationTitle.setText(R.string.home_location_off)
            locationSubtitle.setText(R.string.home_location_off_subtitle)
        }
        is DeviceArea.Found -> {
            locationTitle.text = area.name ?: root.context.getString(R.string.home_location_on)
            locationSubtitle.setText(if (area.name != null) subtitle else R.string.home_location_unknown_subtitle)
        }
    }
}

/** A missing or broken photo keeps the person icon. */
fun ViewMainHeaderBinding.showAvatar(url: String?) {
    profileAvatar.loadWithShimmer(url) {
        error(R.drawable.ic_person)
        fallback(R.drawable.ic_person)
    }
}

/**
 * The top bar of a tab that has no model of its own for it (Favorites, Profile): the area, the notifications and the
 * photo, as on Home and Search. Create it as a property of the Fragment, so the location launchers are registered in
 * time, and call [bind] from `onViewCreated`.
 */
class MainHeader(private val fragment: Fragment, @StringRes private val subtitle: Int) {
    private val viewModel by lazy { ViewModelProvider(fragment)[MainHeaderViewModel::class.java] }

    private val locationAccess = LocationAccess(
        fragment = fragment,
        onPermissionResult = { granted -> viewModel.refreshLocation(openSettingsIfOff = granted) },
        onSettingsClosed = { viewModel.refreshLocation() },
    )

    fun bind(header: ViewMainHeaderBinding) {
        header.locationGroup.clicks { locationAccess.onAreaClicked(viewModel.uiState.value.area) { viewModel.refreshLocation() } }
        fragment.bindNotificationBell(header)

        val owner = fragment.viewLifecycleOwner
        var shownUser: UserProfile? = null
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        header.showArea(state.area, subtitle)
                        // The photo only loads again for another person.
                        if (state.user != null && state.user != shownUser) {
                            shownUser = state.user
                            header.showAvatar(state.user.avatarUrl)
                        }
                    }
                }
                launch { viewModel.openSettings.collect { locationAccess.openLocationSettings() } }
            }
        }
    }
}
