package com.example.huetaste.domain.model

data class Dish(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val mealTimes: List<String>,
    val priceFrom: Int?,
    val priceTo: Int?,
)

data class DishDetail(
    val dish: Dish,
    val categoryName: String?,
    val ingredients: List<DishIngredient>,
    val tastes: List<String>,
    val diets: List<String>,
    val places: List<DishPlace>,
    val isFavorite: Boolean,
    val images: List<GalleryImage>,
)

data class GalleryImage(
    val url: String,
    val thumbnailUrl: String?,
    val source: String?,
)

data class DishIngredient(
    val name: String,
    val group: String?,
    val isMain: Boolean,
)

data class DishPlace(
    val place: Place,
    val price: Int?,
    val isSignature: Boolean,
)

data class Place(
    val id: String,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val openingHours: List<String>,
    val priceLevel: Int?,
    val rating: Double?,
    val imageUrl: String?,
)
