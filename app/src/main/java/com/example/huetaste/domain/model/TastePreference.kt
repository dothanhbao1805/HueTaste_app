package com.example.huetaste.domain.model

/** A diet the backend can filter dishes by, such as vegetarian or low sugar. */
data class DietOption(
    val id: String,
    val name: String,
    val description: String?,
)

enum class TasteTagType { FLAVOR, STYLE }

/** A flavour (spicy, sweet…) or dish style (soup, cake…) a user can like. */
data class TasteTag(
    val id: String,
    val type: TasteTagType,
    val name: String,
)

/** How much a user likes [tagId], from 1 to 5. */
data class TastePreference(
    val tagId: String,
    val level: Int,
)

data class Ingredient(
    val id: String,
    val name: String,
    val group: String?,
)

/** The meals the backend knows; [defaultTime] is the usual start time sent with a meal the user eats. */
enum class MealType(val apiValue: String, val defaultTime: String) {
    BREAKFAST("sang", "06:30"),
    LUNCH("trua", "11:00"),
    SNACK("an_vat", "15:00"),
    DINNER("toi", "18:00"),
    LATE_NIGHT("khuya", "21:30"),
}

/** Budget per meal; [maxVnd] is null when there is no upper limit. */
enum class MealBudget(val maxVnd: Long?) {
    UNDER_30K(30_000),
    FROM_30K_TO_60K(60_000),
    FROM_60K_TO_100K(100_000),
    OVER_100K(null),
}

/** What a user wants from one meal of today; the backend stores it per day and meal. */
data class DailyNeed(
    val meal: MealType,
    val time: String,
    val budget: MealBudget?,
    val avoidIngredientIds: List<String>,
)

/** The catalogs the taste setup chooses from. */
data class TasteSetupOptions(
    val diets: List<DietOption>,
    val tags: List<TasteTag>,
)
