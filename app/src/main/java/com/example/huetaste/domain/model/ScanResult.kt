package com.example.huetaste.domain.model

enum class ScanStatus {
    /** The dish is active in the catalog and [ScanResult.dish] is filled. */
    IN_CATALOG,

    /** A new Huế dish is being researched; poll the scan again. */
    UPDATING,

    /** Research finished without verification; only basic information is available. */
    UNVERIFIED,

    /** Not a Huế specialty, so it was not stored. */
    NOT_SAVED,
}

data class ScanResult(
    val id: String,
    val status: ScanStatus,
    val imageUrl: String,
    val recognizedName: String,
    val confidence: Float?,
    val shortDescription: String?,
    val isHueSpecialty: Boolean,
    val dishId: String?,
    val dish: DishDetail?,
    val similarDishes: List<Dish>,
)
