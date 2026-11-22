package com.example.huetaste.common.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.huetaste.domain.model.DeviceArea

/**
 * The location permission and settings screens shared by the tabs that show the device's area. Create it as a
 * property of the Fragment so the result launchers are registered before the Fragment starts.
 *
 * @param onPermissionResult called after the system dialog, with whether any location permission was granted.
 * @param onSettingsClosed called when the user comes back from a settings screen.
 */
class LocationAccess(
    private val fragment: Fragment,
    onPermissionResult: (granted: Boolean) -> Unit,
    onSettingsClosed: () -> Unit,
) {
    /** A first tap always shows the system dialog; only after that can "no dialog" mean it was denied for good. */
    private var hasRequested = false

    private val permission = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> onPermissionResult(result.values.any { it }) }

    private val settings = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onSettingsClosed()
    }

    fun requestPermission() {
        permission.launch(PERMISSIONS)
    }

    fun openLocationSettings() {
        settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    /**
     * The tap on the area in the header: asks for the permission (or opens the app settings once it was denied for
     * good), opens the location settings when location is off, and calls [refresh] when the area is known.
     */
    fun onAreaClicked(area: DeviceArea?, refresh: () -> Unit) {
        when (area) {
            null -> Unit
            DeviceArea.PermissionMissing -> {
                val deniedForGood = hasRequested && PERMISSIONS.none { fragment.shouldShowRequestPermissionRationale(it) }
                if (deniedForGood) {
                    openAppSettings()
                } else {
                    hasRequested = true
                    requestPermission()
                }
            }
            DeviceArea.LocationOff -> openLocationSettings()
            is DeviceArea.Found -> refresh()
        }
    }

    private fun openAppSettings() {
        val packageUri = Uri.fromParts("package", fragment.requireContext().packageName, null)
        settings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }

    private companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
