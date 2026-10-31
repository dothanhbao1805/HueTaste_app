package com.example.huetaste.domain.model

data class PlaceDetail(
    val place: Place,
    val phone: String?,
    val images: List<GalleryImage>,
    val menu: List<PlaceMenuItem>,
    val isFavorite: Boolean,
)

/** A dish the place serves; [price] is what the place charges, not the reference price of the dish. */
data class PlaceMenuItem(
    val dish: Dish,
    val price: Int?,
    val isSignature: Boolean,
)

data class Review(
    val id: String,
    val rating: Int,
    val content: String?,
    /** ISO-8601 timestamp as sent by the backend. */
    val createdAt: String?,
    val authorName: String?,
    val authorAvatarUrl: String?,
)

/** One page of reviews. [total] counts only reviews written in the app; [averageRating] also includes Google. */
data class ReviewPage(
    val reviews: List<Review>,
    val total: Int,
    val page: Int,
    val averageRating: Double?,
) {
    val hasMore get() = page * PAGE_SIZE < total

    companion object {
        const val PAGE_SIZE = 10
    }
}
