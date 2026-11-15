package com.example.huetaste.domain.model

/** A report the user sent about wrong information of a place, with how the admins handled it. */
data class PlaceReport(
    val id: String,
    val placeId: String?,
    val placeName: String?,
    /** The admins hid the place, usually while handling a report about it. */
    val placeHidden: Boolean,
    val reason: String,
    val description: String?,
    val status: ReportStatus,
    /** Note the admins left for the reporter once the report was handled. */
    val resolutionNote: String?,
    /** ISO-8601 timestamps as sent by the backend. */
    val resolvedAt: String?,
    val createdAt: String?,
    val dishes: List<ReportedDish>,
)

/** A dish of the place menu flagged in a report, with the admins' verdict for it. */
data class ReportedDish(
    val dishId: String,
    val dishName: String?,
    val result: ReportDishResult,
)

enum class ReportStatus { PENDING, RESOLVED }

enum class ReportDishResult { PENDING, VIOLATION, NO_VIOLATION }

/** One page of the user's reports, newest first as the backend sends them. */
data class ReportPage(
    val reports: List<PlaceReport>,
    val total: Int,
    val page: Int,
) {
    val hasMore get() = page * PAGE_SIZE < total

    companion object {
        const val PAGE_SIZE = 20
    }
}
