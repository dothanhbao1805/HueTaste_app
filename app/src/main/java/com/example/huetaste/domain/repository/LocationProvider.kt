package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.GeoPoint

/** The device's approximate location. */
interface LocationProvider {
    /** Null when the location permission is missing or no location could be found in time. */
    suspend fun current(): GeoPoint?
}
