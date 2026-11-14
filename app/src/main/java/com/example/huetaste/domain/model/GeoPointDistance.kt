package com.example.huetaste.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0088

/** Straight-line distance over the Earth's surface (haversine). */
fun GeoPoint.distanceKmTo(other: GeoPoint): Double {
    val latDelta = Math.toRadians(other.latitude - latitude)
    val lngDelta = Math.toRadians(other.longitude - longitude)
    val a = sin(latDelta / 2).pow(2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(lngDelta / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
}
