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

/** One past scan in "Lịch sử scan món". */
data class ScanHistoryItem(
    val id: String,
    val status: ScanStatus,
    val imageUrl: String,
    val name: String,
    val confidence: Float?,
    val description: String?,
    val isHueSpecialty: Boolean,
    val isSaved: Boolean,
    val dishId: String?,
    /** Epoch millis. */
    val createdAt: Long,
)

enum class ScanHistoryFilter(val apiValue: String) {
    ALL("tat_ca"),
    HUE_SPECIALTY("dac_san"),

    /** A Huế dish not yet in the catalog: still being researched or left unverified. */
    UPDATING("dang_cap_nhat"),
    SAVED("da_luu"),
}

/** One page of the history, newest first. */
data class ScanHistoryPage(
    val items: List<ScanHistoryItem>,
    val total: Int,
    val page: Int,
) {
    val hasMore get() = page * PAGE_SIZE < total

    companion object {
        const val PAGE_SIZE = 20
    }
}
