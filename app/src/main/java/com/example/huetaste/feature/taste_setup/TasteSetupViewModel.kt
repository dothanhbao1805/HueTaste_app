package com.example.huetaste.feature.taste_setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.model.MealBudget
import com.example.huetaste.domain.model.MealType
import com.example.huetaste.domain.model.TastePreference
import com.example.huetaste.domain.model.TasteTag
import com.example.huetaste.domain.model.TasteTagType
import com.example.huetaste.domain.usecase.GetTasteSetupOptionsUseCase
import com.example.huetaste.domain.usecase.MarkTasteSetupHandledUseCase
import com.example.huetaste.domain.usecase.SaveDailyNeedsUseCase
import com.example.huetaste.domain.usecase.SaveDietsUseCase
import com.example.huetaste.domain.usecase.SaveTastesUseCase
import com.example.huetaste.domain.usecase.SearchIngredientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The pages of the setup, in order; the first three save to the backend when the user moves on. */
enum class TasteStep { DIET, FLAVOR, DAILY_NEED, SUMMARY }

/** Spiciness maps to the level of the backend "Cay" flavour; [NONE] leaves the flavour out. */
enum class SpiceLevel(val level: Int?) { NONE(null), MILD(2), MEDIUM(3), HUE(5) }

enum class OptionsState { LOADING, READY, FAILED }

data class TasteSetupUiState(
    val step: TasteStep = TasteStep.DIET,
    val optionsState: OptionsState = OptionsState.LOADING,
    val diets: List<DietOption> = emptyList(),
    val flavorTags: List<TasteTag> = emptyList(),
    val styleTags: List<TasteTag> = emptyList(),
    /** The backend "Cay" flavour, shown as the spiciness control instead of a chip; null when the catalog lacks it. */
    val spicyTag: TasteTag? = null,
    val selectedDietIds: Set<String> = emptySet(),
    val spiceLevel: SpiceLevel = SpiceLevel.MEDIUM,
    val selectedTagIds: Set<String> = emptySet(),
    val meals: Set<MealType> = DEFAULT_MEALS,
    val budget: MealBudget? = MealBudget.FROM_30K_TO_60K,
    val ingredientResults: List<Ingredient> = emptyList(),
    val isSearchingIngredients: Boolean = false,
    val avoidIngredients: List<Ingredient> = emptyList(),
    val isSaving: Boolean = false,
) {
    val selectedDiets get() = diets.filter { it.id in selectedDietIds }
    val selectedTags get() = (flavorTags + styleTags).filter { it.id in selectedTagIds }

    companion object {
        val DEFAULT_MEALS = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
    }
}

/** One-off results of the setup. */
sealed interface TasteSetupEvent {
    data object OpenMain : TasteSetupEvent
    data class Failed(val network: Boolean, val message: String?) : TasteSetupEvent
}

@HiltViewModel
class TasteSetupViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getOptions: GetTasteSetupOptionsUseCase,
    private val searchIngredients: SearchIngredientsUseCase,
    private val saveDiets: SaveDietsUseCase,
    private val saveTastes: SaveTastesUseCase,
    private val saveDailyNeeds: SaveDailyNeedsUseCase,
    private val markHandled: MarkTasteSetupHandledUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<TasteSetupUiState> = _uiState.asStateFlow()

    private val _events = Channel<TasteSetupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val ingredientQuery = MutableStateFlow("")
    private var searchJob: Job? = null
    private var saveJob: Job? = null

    init {
        loadOptions()
        if (_uiState.value.step == TasteStep.DAILY_NEED) startIngredientSearch()
    }

    fun loadOptions() {
        _uiState.update { it.copy(optionsState = OptionsState.LOADING) }
        viewModelScope.launch {
            try {
                val options = getOptions()
                val flavors = options.tags.filter { it.type == TasteTagType.FLAVOR }
                val spicy = flavors.firstOrNull { it.name.equals(SPICY_TAG_NAME, ignoreCase = true) }
                update {
                    it.copy(
                        optionsState = OptionsState.READY,
                        diets = options.diets,
                        flavorTags = flavors - setOfNotNull(spicy),
                        styleTags = options.tags.filter { tag -> tag.type == TasteTagType.STYLE },
                        spicyTag = spicy,
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(optionsState = OptionsState.FAILED) }
            }
        }
    }

    // region Selections

    fun selectNormalDiet() = update { it.copy(selectedDietIds = emptySet()) }

    fun toggleDiet(id: String) = update { it.copy(selectedDietIds = it.selectedDietIds.toggle(id)) }

    fun selectSpice(level: SpiceLevel) = update { it.copy(spiceLevel = level) }

    fun toggleTag(id: String) = update { it.copy(selectedTagIds = it.selectedTagIds.toggle(id)) }

    fun toggleMeal(meal: MealType) = update { it.copy(meals = it.meals.toggle(meal)) }

    /** Picking the selected budget again clears it, which sends no limit. */
    fun selectBudget(budget: MealBudget) = update { it.copy(budget = budget.takeIf { b -> b != it.budget }) }

    fun toggleAvoid(ingredient: Ingredient) = update { state ->
        val current = state.avoidIngredients
        when {
            current.any { it.id == ingredient.id } -> state.copy(avoidIngredients = current.filterNot { it.id == ingredient.id })
            current.size >= MAX_AVOID -> state
            else -> state.copy(avoidIngredients = current + ingredient)
        }
    }

    fun searchIngredient(query: String) {
        ingredientQuery.value = query
    }

    // endregion

    // region Navigation

    /** The footer's main button: saves the current page and moves on, or leaves the setup from the summary. */
    fun continueStep() {
        val state = _uiState.value
        when (state.step) {
            TasteStep.DIET -> save(TasteStep.FLAVOR) { saveDiets(state.selectedDietIds.toList()) }
            TasteStep.FLAVOR -> save(TasteStep.DAILY_NEED) { saveTastes(state.toTastes()) }
            TasteStep.DAILY_NEED -> save(TasteStep.SUMMARY) {
                saveDailyNeeds(state.toDailyNeeds())
                markHandled()
            }
            TasteStep.SUMMARY -> _events.trySend(TasteSetupEvent.OpenMain)
        }
    }

    /** Leaves the setup for now; it is not offered again on this device. */
    fun skip() {
        if (saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            markHandled()
            _events.send(TasteSetupEvent.OpenMain)
        }
    }

    /** Back steps through the pages; on the first page it skips, on the summary everything is already saved. */
    fun back() {
        if (saveJob?.isActive == true) return
        when (val step = _uiState.value.step) {
            TasteStep.DIET -> skip()
            TasteStep.SUMMARY -> _events.trySend(TasteSetupEvent.OpenMain)
            else -> update { it.copy(step = TasteStep.entries[step.ordinal - 1]) }
        }
    }

    /** "Chỉnh lại" on the summary: start over with the current choices kept. */
    fun editAgain() = update { it.copy(step = TasteStep.DIET) }

    private fun save(next: TasteStep, block: suspend () -> Unit) {
        if (saveJob?.isActive == true) return
        _uiState.update { it.copy(isSaving = true) }
        saveJob = viewModelScope.launch {
            try {
                block()
                update { it.copy(isSaving = false, step = next) }
                if (next == TasteStep.DAILY_NEED) startIngredientSearch()
            } catch (e: AppError) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(TasteSetupEvent.Failed(e.code == AppError.NETWORK, e.message))
            }
        }
    }

    // endregion

    /** Ingredients load only once the user reaches the page that needs them; a newer query cancels the older one. */
    @OptIn(FlowPreview::class)
    private fun startIngredientSearch() {
        if (searchJob != null) return
        searchJob = viewModelScope.launch {
            ingredientQuery.debounce(SEARCH_DEBOUNCE_MS).collectLatest { query ->
                _uiState.update { it.copy(isSearchingIngredients = true) }
                val results = try {
                    searchIngredients(query)
                } catch (e: AppError) {
                    emptyList()
                }
                _uiState.update { it.copy(ingredientResults = results, isSearchingIngredients = false) }
            }
        }
    }

    private fun TasteSetupUiState.toTastes(): List<TastePreference> {
        val liked = selectedTagIds.map { TastePreference(it, LIKED_LEVEL) }
        val spice = spicyTag?.let { tag -> spiceLevel.level?.let { TastePreference(tag.id, it) } }
        return liked + listOfNotNull(spice)
    }

    private fun TasteSetupUiState.toDailyNeeds(): List<DailyNeed> = meals.sortedBy { it.ordinal }.map { meal ->
        DailyNeed(meal, meal.defaultTime, budget, avoidIngredients.map { it.id })
    }

    // region Saved state

    /** Applies [transform] and keeps the choices in the saved state so they survive process death. */
    private fun update(transform: (TasteSetupUiState) -> TasteSetupUiState) {
        val state = _uiState.updateAndGet(transform)
        savedState[KEY_STEP] = state.step.name
        savedState[KEY_DIETS] = ArrayList(state.selectedDietIds)
        savedState[KEY_SPICE] = state.spiceLevel.name
        savedState[KEY_TAGS] = ArrayList(state.selectedTagIds)
        savedState[KEY_MEALS] = ArrayList(state.meals.map { it.name })
        savedState[KEY_BUDGET] = state.budget?.name
        savedState[KEY_AVOID_IDS] = ArrayList(state.avoidIngredients.map { it.id })
        savedState[KEY_AVOID_NAMES] = ArrayList(state.avoidIngredients.map { it.name })
    }

    private fun restoreState(): TasteSetupUiState {
        val step = savedState.get<String>(KEY_STEP) ?: return TasteSetupUiState()
        val avoidIds = savedState.get<ArrayList<String>>(KEY_AVOID_IDS).orEmpty()
        val avoidNames = savedState.get<ArrayList<String>>(KEY_AVOID_NAMES).orEmpty()
        return TasteSetupUiState(
            step = TasteStep.valueOf(step),
            selectedDietIds = savedState.get<ArrayList<String>>(KEY_DIETS).orEmpty().toSet(),
            spiceLevel = savedState.get<String>(KEY_SPICE)?.let(SpiceLevel::valueOf) ?: SpiceLevel.MEDIUM,
            selectedTagIds = savedState.get<ArrayList<String>>(KEY_TAGS).orEmpty().toSet(),
            meals = savedState.get<ArrayList<String>>(KEY_MEALS).orEmpty().map(MealType::valueOf).toSet(),
            budget = savedState.get<String>(KEY_BUDGET)?.let(MealBudget::valueOf),
            avoidIngredients = avoidIds.zip(avoidNames) { id, name -> Ingredient(id, name, null) },
        )
    }

    // endregion

    private fun <T> Set<T>.toggle(item: T) = if (item in this) this - item else this + item

    private companion object {
        const val SPICY_TAG_NAME = "Cay"
        const val LIKED_LEVEL = 4
        const val MAX_AVOID = 30
        const val SEARCH_DEBOUNCE_MS = 300L

        const val KEY_STEP = "taste_step"
        const val KEY_DIETS = "taste_diets"
        const val KEY_SPICE = "taste_spice"
        const val KEY_TAGS = "taste_tags"
        const val KEY_MEALS = "taste_meals"
        const val KEY_BUDGET = "taste_budget"
        const val KEY_AVOID_IDS = "taste_avoid_ids"
        const val KEY_AVOID_NAMES = "taste_avoid_names"
    }
}
