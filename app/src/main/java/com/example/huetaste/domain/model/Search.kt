package com.example.huetaste.domain.model

/** The `sapXep` values of `GET /mon-an`. */
enum class DishSort(val code: String) { POPULAR("pho_bien"), NAME("ten"), PRICE_LOW("gia_thap") }

/** The `khuVuc` values of `GET /dia-diem`. */
enum class PlaceArea(val code: String) { CO_DO("co_do"), BO_BAC("bo_bac"), BO_NAM("bo_nam") }

data class DishSearchQuery(
    val text: String,
    val categoryId: String?,
    val ingredients: List<String>,
    val sort: DishSort,
)

/** [near] and [radiusKm] go together: with a point the backend keeps places inside the radius, nearest first. */
data class PlaceSearchQuery(
    val text: String,
    val near: GeoPoint?,
    val radiusKm: Double,
    val area: PlaceArea?,
)

data class DishSearchItem(
    val dish: Dish,
    /** Active places that sell the dish. */
    val placeCount: Int,
    /** The dish is the signature dish of at least one place. */
    val isSignature: Boolean,
)

data class PlaceSearchItem(
    val place: Place,
    val distanceKm: Double?,
    val area: PlaceArea?,
)

data class SearchPage<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
) {
    val hasMore get() = page * PAGE_SIZE < total

    companion object {
        const val PAGE_SIZE = 20
    }
}
