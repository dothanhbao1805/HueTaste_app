package com.example.huetaste.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.model.DishSuggestion
import com.example.huetaste.domain.model.MealType
import com.example.huetaste.domain.model.SuggestionContext
import com.example.huetaste.domain.model.SuggestionRequest
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetCurrentUserUseCase
import com.example.huetaste.domain.usecase.GetDishCategoriesUseCase
import com.example.huetaste.domain.usecase.GetDishSuggestionsUseCase
import com.example.huetaste.domain.usecase.GetDishesUseCase
import com.example.huetaste.domain.usecase.GetFavoriteDishIdsUseCase
import com.example.huetaste.domain.usecase.SetDishFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface HomeDishesState {
    object Loading : HomeDishesState
    object Empty : HomeDishesState
    data class Error(val error: AppError) : HomeDishesState
    data class Content(val suggestions: List<DishSuggestion>) : HomeDishesState
}

/** One card of the suggested dishes; [matchPercent] is null for guests, whom nothing is scored for. */
data class HomeDishItem(
    val dish: Dish,
    val matchPercent: Int?,
    val reason: String?,
    val categoryName: String?,
    val isFavorite: Boolean,
    val canFavorite: Boolean,
)

data class HomeUiState(
    val isSignedIn: Boolean = false,
    val user: UserProfile? = null,
    val selectedMeal: MealType,
    val context: SuggestionContext? = null,
    val dishes: HomeDishesState = HomeDishesState.Loading,
    val categories: List<DishCategory> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
) {
    val dishItems: List<HomeDishItem>
        get() {
            val loaded = dishes as? HomeDishesState.Content ?: return emptyList()
            val categoryNames = categories.associate { it.id to it.name }
            return loaded.suggestions.map { suggestion ->
                HomeDishItem(
                    dish = suggestion.dish,
                    matchPercent = suggestion.score?.let { (it * 100).roundToInt().coerceIn(0, 100) },
                    reason = suggestion.reason ?: suggestion.dish.description?.takeIf { it.isNotBlank() },
                    categoryName = categoryNames[suggestion.dish.categoryId],
                    isFavorite = suggestion.dish.id in favoriteIds,
                    canFavorite = isSignedIn,
                )
            }
        }
}

sealed interface HomeEvent {
    object FavoriteFailed : HomeEvent
}

/**
 * Loads the home data once, so rotating or switching tabs neither reloads it nor records another suggestion on the
 * backend. Dishes, categories and favorites load independently: one failing leaves the other sections on screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getAuthState: GetAuthStateUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getSuggestions: GetDishSuggestionsUseCase,
    private val getDishes: GetDishesUseCase,
    private val getCategories: GetDishCategoriesUseCase,
    private val getFavoriteIds: GetFavoriteDishIdsUseCase,
    private val setDishFavorite: SetDishFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(selectedMeal = restoredMeal()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var dishesJob: Job? = null

    /** What the list on screen answers; "Thử lại" asks it again. */
    private var lastRequest = SuggestionRequest()

    init {
        viewModelScope.launch {
            val signedIn = getAuthState() == AuthState.SIGNED_IN
            val user = if (signedIn) getCurrentUser() else null
            _uiState.update { it.copy(isSignedIn = signedIn, user = user) }
            loadDishes()
            loadCategories()
            if (signedIn) loadFavorites()
        }
    }

    fun selectMeal(meal: MealType) {
        savedState[KEY_MEAL] = meal.name
        _uiState.update { it.copy(selectedMeal = meal) }
    }

    /** @return false for a guest, because the backend only suggests to signed-in users. */
    fun suggest(budget: Int, note: String): Boolean {
        val state = _uiState.value
        if (!state.isSignedIn) return false
        lastRequest = SuggestionRequest(
            meal = state.selectedMeal,
            budget = budget,
            note = note.trim().takeIf { it.isNotEmpty() },
        )
        loadDishes()
        return true
    }

    fun retry() {
        loadDishes()
        if (_uiState.value.categories.isEmpty()) loadCategories()
    }

    /** Applied at once and undone if the backend refuses. */
    fun toggleFavorite(dishId: String) {
        val favorite = dishId !in _uiState.value.favoriteIds
        setFavoriteShown(dishId, favorite)
        viewModelScope.launch {
            try {
                setDishFavorite(dishId, favorite)
            } catch (e: AppError) {
                setFavoriteShown(dishId, !favorite)
                _events.send(HomeEvent.FavoriteFailed)
            }
        }
    }

    private fun setFavoriteShown(dishId: String, favorite: Boolean) {
        _uiState.update { state ->
            state.copy(favoriteIds = if (favorite) state.favoriteIds + dishId else state.favoriteIds - dishId)
        }
    }

    private fun loadDishes() {
        dishesJob?.cancel()
        _uiState.update { it.copy(dishes = HomeDishesState.Loading) }
        dishesJob = viewModelScope.launch {
            try {
                if (_uiState.value.isSignedIn) {
                    val result = getSuggestions(lastRequest)
                    _uiState.update { it.copy(context = result.context, dishes = dishesState(result.dishes)) }
                } else {
                    val dishes = getDishes(GUEST_DISH_COUNT).map { DishSuggestion(it, score = null, reason = null) }
                    _uiState.update { it.copy(dishes = dishesState(dishes)) }
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(dishes = HomeDishesState.Error(e)) }
            }
        }
    }

    private fun dishesState(dishes: List<DishSuggestion>): HomeDishesState =
        if (dishes.isEmpty()) HomeDishesState.Empty else HomeDishesState.Content(dishes)

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = getCategories()
                _uiState.update { it.copy(categories = categories) }
            } catch (e: AppError) {
                // The section stays hidden; "Thử lại" on the dishes asks again.
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                val ids = getFavoriteIds()
                _uiState.update { it.copy(favoriteIds = ids) }
            } catch (e: AppError) {
                // The hearts stay empty; tapping one still saves it.
            }
        }
    }

    private fun restoredMeal(): MealType =
        savedState.get<String>(KEY_MEAL)?.let { name -> MealType.entries.firstOrNull { it.name == name } }
            ?: mealForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    /** The same hours as the backend; the home chips have no late-night meal, so that time picks dinner. */
    private fun mealForHour(hour: Int): MealType = when (hour) {
        in 5..9 -> MealType.BREAKFAST
        in 10..13 -> MealType.LUNCH
        in 14..16 -> MealType.SNACK
        else -> MealType.DINNER
    }

    private companion object {
        const val KEY_MEAL = "home_selected_meal"
        const val GUEST_DISH_COUNT = 10
    }
}
