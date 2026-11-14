package com.example.huetaste.domain.model

/** What can be said about where the device is. */
sealed interface DeviceArea {
    object PermissionMissing : DeviceArea
    object LocationOff : DeviceArea

    /**
     * [name] is null when the area could not be named and [point] when no recent position was found.
     * [isPrecise] is false when the user only allowed an approximate position.
     */
    data class Found(val name: String?, val point: GeoPoint?, val isPrecise: Boolean) : DeviceArea
}
