package com.example.huetaste.common.navigation

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.MealType
import com.example.huetaste.domain.model.SuggestionRequest

/**
 * What the user picked on Home, carried through the review screen to the results. Intent extras reach each screen's
 * ViewModel through its [SavedStateHandle], so they survive recreation.
 */
data class SuggestionArgs(
    val meal: MealType?,
    val budget: Int?,
    val note: String?,
    val point: GeoPoint?,
) {
    fun putInto(intent: Intent): Intent = intent.apply {
        meal?.let { putExtra(KEY_MEAL, it.name) }
        budget?.let { putExtra(KEY_BUDGET, it) }
        note?.let { putExtra(KEY_NOTE, it) }
        point?.let {
            putExtra(KEY_LATITUDE, it.latitude)
            putExtra(KEY_LONGITUDE, it.longitude)
        }
    }

    fun toRequest(placeCount: Int) = SuggestionRequest(meal, budget, note, point, placeCount)

    companion object {
        private const val KEY_MEAL = "suggestion_meal"
        private const val KEY_BUDGET = "suggestion_budget"
        private const val KEY_NOTE = "suggestion_note"
        private const val KEY_LATITUDE = "suggestion_latitude"
        private const val KEY_LONGITUDE = "suggestion_longitude"

        fun from(state: SavedStateHandle): SuggestionArgs {
            val latitude = state.get<Double>(KEY_LATITUDE)
            val longitude = state.get<Double>(KEY_LONGITUDE)
            return SuggestionArgs(
                meal = state.get<String>(KEY_MEAL)?.let { name -> MealType.entries.firstOrNull { it.name == name } },
                budget = state.get<Int>(KEY_BUDGET),
                note = state.get<String>(KEY_NOTE),
                point = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
            )
        }
    }
}
