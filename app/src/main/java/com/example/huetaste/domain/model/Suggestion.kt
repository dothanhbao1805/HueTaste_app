package com.example.huetaste.domain.model

/**
 * What the user asked for; a null field lets the backend use the time of day and today's saved needs.
 * [point] lets the backend score places by distance; [placeCount] is 0 when only dishes are wanted.
 */
data class SuggestionRequest(
    val meal: MealType? = null,
    val budget: Int? = null,
    val note: String? = null,
    val point: GeoPoint? = null,
    val placeCount: Int = 0,
)

/** [weather] is the backend code `nong`, `mat`, `lanh` or `mua`. */
data class SuggestionContext(
    val weather: String?,
    val temperature: Double?,
)

/** [score] is how well the dish fits the user, from 0 to 1, and null when nothing was scored for this user. */
data class DishSuggestion(
    val dish: Dish,
    val score: Double?,
    val reason: String?,
)

/** A place serving some of the best-scored dishes; [relatedDishes] are those dishes, best first. */
data class PlaceSuggestion(
    val place: Place,
    val distanceKm: Double?,
    val score: Double?,
    val reason: String?,
    val relatedDishes: List<Dish>,
)

data class SuggestionResult(
    val context: SuggestionContext?,
    val dishes: List<DishSuggestion>,
    val places: List<PlaceSuggestion> = emptyList(),
)

data class DishCategory(
    val id: String,
    val name: String,
)
