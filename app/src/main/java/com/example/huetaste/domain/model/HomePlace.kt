package com.example.huetaste.domain.model

/** [distanceKm] is null when the list was not built around the user's position. */
data class HomePlace(
    val place: Place,
    val distanceKm: Double?,
)

enum class HomePlacesKind { NEARBY, FEATURED }

data class HomePlaces(
    val kind: HomePlacesKind,
    val places: List<HomePlace>,
)
