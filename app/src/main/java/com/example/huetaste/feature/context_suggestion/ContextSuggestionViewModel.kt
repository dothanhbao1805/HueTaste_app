package com.example.huetaste.feature.context_suggestion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.common.navigation.SuggestionArgs
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.Place
import com.example.huetaste.domain.model.SuggestionResult
import com.example.huetaste.domain.usecase.GetDishSuggestionsUseCase
import com.example.huetaste.domain.usecase.GetFavoriteDishIdsUseCase
import com.example.huetaste.domain.usecase.SetDishFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

enum class SuggestionTab { DISHES, PLACES }

/** [rank] starts at 1; [matchPercent] is null when the backend sent no score. */
data class SuggestedDishItem(
    val dish: Dish,
    val rank: Int,
    val matchPercent: Int?,
    val reason: String?,
    val isFavorite: Boolean,
)

data class SuggestedPlaceItem(
    val place: Place,
    val rank: Int,
    val matchPercent: Int?,
    val reason: String?,
    val distanceKm: Double?,
    val relatedDishes: List<String>,
)

data class ContextSuggestionUiState(
    val args: SuggestionArgs,
    val tab: SuggestionTab,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val result: SuggestionResult? = null,
    val favoriteIds: Set<String> = emptySet(),
) {
    val dishItems: List<SuggestedDishItem>
        get() = result?.dishes.orEmpty().mapIndexed { index, it ->
            SuggestedDishItem(it.dish, index + 1, it.score.toPercent(), it.reason, it.dish.id in favoriteIds)
        }

    val placeItems: List<SuggestedPlaceItem>
        get() = result?.places.orEmpty().mapIndexed { index, it ->
            SuggestedPlaceItem(
                place = it.place,
                rank = index + 1,
                matchPercent = it.score.toPercent(),
                reason = it.reason,
                distanceKm = it.distanceKm,
                relatedDishes = it.relatedDishes.take(RELATED_DISHES_SHOWN).map { dish -> dish.name },
            )
        }

    private fun Double?.toPercent() = this?.let { (it * 100).roundToInt().coerceIn(0, 100) }

    private companion object {
        const val RELATED_DISHES_SHOWN = 3
    }
}

sealed interface ContextSuggestionEvent {
    object FavoriteFailed : ContextSuggestionEvent
}

/**
 * Asks `/goi-y` once for both tabs: every call is recorded on the backend, so rotating or switching tabs never asks
 * again. Only the selected tab is kept across process death; the list is asked for again then.
 */
@HiltViewModel
class ContextSuggestionViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getSuggestions: GetDishSuggestionsUseCase,
    private val getFavoriteIds: GetFavoriteDishIdsUseCase,
    private val setDishFavorite: SetDishFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ContextSuggestionUiState(
            args = SuggestionArgs.from(savedState),
            tab = savedState.get<String>(KEY_TAB)?.let(SuggestionTab::valueOf) ?: SuggestionTab.DISHES,
        ),
    )
    val uiState: StateFlow<ContextSuggestionUiState> = _uiState.asStateFlow()

    private val _events = Channel<ContextSuggestionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
        loadFavorites()
    }

    fun selectTab(tab: SuggestionTab) {
        savedState[KEY_TAB] = tab.name
        _uiState.update { it.copy(tab = tab) }
    }

    fun retry() {
        if (!_uiState.value.isLoading) load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = getSuggestions(_uiState.value.args.toRequest(placeCount = PLACE_COUNT))
                _uiState.update { it.copy(isLoading = false, result = result) }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    /** Hearts only; when this fails the cards just start unfilled. */
    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                val ids = getFavoriteIds()
                _uiState.update { it.copy(favoriteIds = ids) }
            } catch (e: AppError) {
                // Nothing to show: the hearts stay unfilled.
            }
        }
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
                _events.send(ContextSuggestionEvent.FavoriteFailed)
            }
        }
    }

    private fun setFavoriteShown(dishId: String, favorite: Boolean) {
        _uiState.update { state ->
            state.copy(favoriteIds = if (favorite) state.favoriteIds + dishId else state.favoriteIds - dishId)
        }
    }

    private companion object {
        const val KEY_TAB = "context_suggestion_tab"
        const val PLACE_COUNT = 10
    }
}
