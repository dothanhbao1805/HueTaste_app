package com.example.huetaste.domain.model

/** The signed-in user's saved dishes and places, each list newest first as the backend returns them. */
data class Favorites(
    val dishes: List<Dish>,
    val places: List<Place>,
)
