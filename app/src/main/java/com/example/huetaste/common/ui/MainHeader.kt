package com.example.huetaste.common.ui

import androidx.annotation.StringRes
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
