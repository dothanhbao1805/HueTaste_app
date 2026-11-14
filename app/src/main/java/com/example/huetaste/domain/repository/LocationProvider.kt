package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.model.GeoPoint

/** The device's approximate location. */
interface LocationProvider {
    /** Null when the location permission is missing or no location could be found in time. */
    suspend fun current(): GeoPoint?

    /** The named area around the device, for showing to the user. */
    suspend fun currentArea(): DeviceArea

    /** True only the first time it is called on this device: the app asks for the permission unprompted just once. */
    suspend fun claimPermissionPrompt(): Boolean
}
