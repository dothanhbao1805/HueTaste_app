package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.HomePlaces
import com.example.huetaste.domain.model.HomePlacesKind
import com.example.huetaste.domain.model.distanceKmTo
import com.example.huetaste.domain.repository.PlaceRepository
import javax.inject.Inject

/**
 * Places near a user who is in Huế; anyone else, and a user in Huế with nothing around them, gets the featured places.
 * Being in Huế is decided from coordinates, because the area names of the device's geocoder vary and can be missing.
 */
class GetHomePlacesUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(point: GeoPoint?): HomePlaces {
        if (point != null && point.distanceKmTo(HUE_CENTER) < HUE_RADIUS_KM) {
            val nearby = repository.getNearbyPlaces(point, NEARBY_RADIUS_KM, PLACE_COUNT)
            if (nearby.isNotEmpty()) return HomePlaces(HomePlacesKind.NEARBY, nearby)
        }
        return HomePlaces(HomePlacesKind.FEATURED, repository.getFeaturedPlaces(PLACE_COUNT))
    }

    private companion object {
        /** The same center as the backend's `HUE_VI_DO` and `HUE_KINH_DO`. */
        val HUE_CENTER = GeoPoint(16.4637, 107.5909)
        const val HUE_RADIUS_KM = 40.0
        const val NEARBY_RADIUS_KM = 10.0
        const val PLACE_COUNT = 5
    }
}
