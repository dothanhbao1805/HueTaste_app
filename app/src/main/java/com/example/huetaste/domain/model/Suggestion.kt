package com.example.huetaste.domain.model

/** What the user asked for; a null field lets the backend use the time of day and today's saved needs. */
data class SuggestionRequest(
    val meal: MealType? = null,
    val budget: Int? = null,
    val note: String? = null,
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

data class SuggestionResult(
    val context: SuggestionContext?,
    val dishes: List<DishSuggestion>,
)

data class DishCategory(
    val id: String,
    val name: String,
)
