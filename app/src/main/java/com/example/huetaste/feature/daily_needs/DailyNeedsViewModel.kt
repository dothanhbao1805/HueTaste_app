package com.example.huetaste.feature.daily_needs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.model.MealBudget
import com.example.huetaste.domain.model.MealType
import com.example.huetaste.domain.usecase.GetTodayNeedsUseCase
import com.example.huetaste.domain.usecase.SaveDailyNeedsUseCase
import com.example.huetaste.feature.taste_setup.OptionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyNeedsUiState(
    val optionsState: OptionsState = OptionsState.LOADING,
    val meals: Set<MealType> = emptySet(),
    /** Null is "Không giới hạn". */
    val budget: MealBudget? = null,
    val note: String = "",
    /** Today's needs as the backend holds them; the screen is dirty when the choices differ from them. */
    val saved: List<DailyNeed> = emptyList(),
    val isSaving: Boolean = false,
) {
    private val savedMeals get() = saved.map { it.meal }.toSet()

    // The screen edits one budget and note for the whole day; the backend keeps them per meal.
    private val savedBudget get() = saved.firstOrNull()?.budget
    private val savedNote get() = saved.firstOrNull()?.note.orEmpty()

    val isDirty get() = meals != savedMeals || budget != savedBudget || note.trim() != savedNote.trim()
}

sealed interface DailyNeedsEvent {
    data object Saved : DailyNeedsEvent
    data class SaveFailed(val network: Boolean) : DailyNeedsEvent
}

/** Edits today's meals, budget per meal and note (`GET/PUT/DELETE /users/me/nhu-cau`). */
@HiltViewModel
class DailyNeedsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getTodayNeeds: GetTodayNeedsUseCase,
    private val saveDailyNeeds: SaveDailyNeedsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyNeedsUiState())
    val uiState: StateFlow<DailyNeedsUiState> = _uiState.asStateFlow()

    private val _events = Channel<DailyNeedsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(optionsState = OptionsState.LOADING) }
        viewModelScope.launch {
            try {
                val saved = getTodayNeeds()
                // Choices made before the process died win over what the backend holds.
                val meals = savedState.get<ArrayList<String>>(KEY_MEALS)?.map(MealType::valueOf)?.toSet()
                    ?: saved.map { it.meal }.toSet()
                val budget = if (savedState.contains(KEY_BUDGET)) {
                    savedState.get<String>(KEY_BUDGET)?.let(MealBudget::valueOf)
                } else {
                    saved.firstOrNull()?.budget
                }
                val note = savedState.get<String>(KEY_NOTE) ?: saved.firstOrNull()?.note.orEmpty()
                _uiState.update {
                    it.copy(optionsState = OptionsState.READY, meals = meals, budget = budget, note = note, saved = saved)
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(optionsState = OptionsState.FAILED) }
            }
        }
    }

    fun toggleMeal(meal: MealType) = edit {
        it.copy(meals = if (meal in it.meals) it.meals - meal else it.meals + meal)
    }

    /** Tapping the selected budget again clears it. */
    fun selectBudget(budget: MealBudget) = edit { it.copy(budget = budget.takeUnless { b -> b == it.budget }) }

    fun setNote(note: String) {
        if (note != _uiState.value.note) edit { it.copy(note = note.take(NOTE_MAX_LENGTH)) }
    }

    /** "+ Không cay" and the like: appended to the note, comma separated. */
    fun appendToNote(phrase: String) {
        val note = _uiState.value.note.trim()
        setNote(if (note.isEmpty()) phrase else "$note, ${phrase.lowercase()}")
    }

    /** "Xóa lựa chọn": clears the screen; nothing is deleted until the user saves. */
    fun clearAll() = edit { it.copy(meals = emptySet(), budget = null, note = "") }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || !state.isDirty) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                saveDailyNeeds(state.toNeeds(), replacing = state.saved)
                _uiState.update { it.copy(isSaving = false) }
                _events.send(DailyNeedsEvent.Saved)
            } catch (e: AppError) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(DailyNeedsEvent.SaveFailed(network = e.code == AppError.NETWORK))
            }
        }
    }

    /**
     * One need per chosen meal. Each PUT replaces the whole row, so the meal time and the ingredients to avoid
     * (picked in the taste setup, not editable here) are carried over from what is saved.
     */
    private fun DailyNeedsUiState.toNeeds(): List<DailyNeed> {
        val avoid = saved.firstOrNull()?.avoidIngredientIds.orEmpty()
        val note = note.trim().ifEmpty { null }
        return meals.sortedBy { it.ordinal }.map { meal ->
            saved.firstOrNull { it.meal == meal }?.copy(budget = budget, note = note)
                ?: DailyNeed(meal, meal.defaultTime, budget, avoid, note)
        }
    }

    private fun edit(transform: (DailyNeedsUiState) -> DailyNeedsUiState) {
        val state = _uiState.updateAndGet(transform)
        savedState[KEY_MEALS] = ArrayList(state.meals.map { it.name })
        savedState[KEY_BUDGET] = state.budget?.name
        savedState[KEY_NOTE] = state.note
    }

    companion object {
        /** The design's limit; the backend accepts up to 500. */
        const val NOTE_MAX_LENGTH = 200

        private const val KEY_MEALS = "daily_needs_meals"
        private const val KEY_BUDGET = "daily_needs_budget"
        private const val KEY_NOTE = "daily_needs_note"
    }
}
