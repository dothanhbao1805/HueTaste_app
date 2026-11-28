package com.example.huetaste.feature.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.common.navigation.SuggestionArgs
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.model.DishSuggestion
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.HomePlaces
import com.example.huetaste.domain.model.Place
import com.example.huetaste.domain.model.SuggestionContext
import com.example.huetaste.domain.model.SuggestionRequest
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.model.distanceKmTo
import com.example.huetaste.domain.usecase.ClaimLocationPromptUseCase
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetCurrentAreaUseCase
import com.example.huetaste.domain.usecase.GetCurrentUserUseCase
import com.example.huetaste.domain.usecase.GetDishCategoriesUseCase
import com.example.huetaste.domain.usecase.GetDishSuggestionsUseCase
import com.example.huetaste.domain.usecase.GetDishesUseCase
import com.example.huetaste.domain.usecase.GetFavoriteDishIdsUseCase
import com.example.huetaste.domain.usecase.GetHomePlacesUseCase
import com.example.huetaste.domain.usecase.SetDishFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface HomeDishesState {
    object Loading : HomeDishesState
    object Empty : HomeDishesState
    data class Error(val error: AppError) : HomeDishesState
    data class Content(val suggestions: List<DishSuggestion>) : HomeDishesState
}

sealed interface HomePlacesState {
    object Loading : HomePlacesState
    data class Error(val error: AppError) : HomePlacesState

    /** [area] is what the list was shown for, so the texts around it stay put while the location is refreshed. */
    data class Content(val places: HomePlaces, val area: DeviceArea) : HomePlacesState
}

data class HomePlaceItem(
    val place: Place,
    val distanceKm: Double?,
    val isDistanceApproximate: Boolean,
)

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
    /** Null while the device's area is being found. */
    val location: DeviceArea? = null,
    val context: SuggestionContext? = null,
    val dishes: HomeDishesState = HomeDishesState.Loading,
    val categories: List<DishCategory> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val places: HomePlacesState = HomePlacesState.Loading,
) {
    val placeItems: List<HomePlaceItem>
        get() {
            val loaded = places as? HomePlacesState.Content ?: return emptyList()
            val isApproximate = (loaded.area as? DeviceArea.Found)?.isPrecise == false
            return loaded.places.places.map { HomePlaceItem(it.place, it.distanceKm, isApproximate) }
        }

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
    object AskLocationPermission : HomeEvent
    object OpenLocationSettings : HomeEvent
}

/**
 * Loads the home data once, so rotating or switching tabs neither reloads it nor records another suggestion on the
 * backend. Dishes, categories and favorites load independently: one failing leaves the other sections on screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAuthState: GetAuthStateUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getSuggestions: GetDishSuggestionsUseCase,
    private val getDishes: GetDishesUseCase,
    private val getCategories: GetDishCategoriesUseCase,
    private val getFavoriteIds: GetFavoriteDishIdsUseCase,
    private val setDishFavorite: SetDishFavoriteUseCase,
    private val getCurrentArea: GetCurrentAreaUseCase,
    private val claimLocationPrompt: ClaimLocationPromptUseCase,
    private val getHomePlaces: GetHomePlacesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var dishesJob: Job? = null
    private var locationJob: Job? = null
    private var placesJob: Job? = null

    /** When the location was last read, on the clock that keeps running while the device sleeps; 0 means not yet. */
    private var locatedAt = 0L

    /** The position the place list on screen was loaded for. */
    private var placesPoint: GeoPoint? = null

    init {
        loadLocation(askPermissionIfFirstTime = true)
        viewModelScope.launch {
            getCurrentUser.observe().filterNotNull().collect { user -> _uiState.update { it.copy(user = user) } }
        }
        viewModelScope.launch {
            val signedIn = getAuthState() == AuthState.SIGNED_IN
            _uiState.update { it.copy(isSignedIn = signedIn) }
            loadDishes()
            loadCategories()
            if (signedIn) loadFavorites()
        }
    }

    /** @param openSettingsIfOff true right after the permission was granted, when device location is the last step. */
    fun refreshLocation(openSettingsIfOff: Boolean = false) {
        loadLocation(openSettingsIfOff = openSettingsIfOff)
    }

    /** A session starts with a new ViewModel, or when the app comes back after being away for a long while. */
    fun onScreenStarted() {
        if (locatedAt != 0L && SystemClock.elapsedRealtime() - locatedAt >= SESSION_GAP_MS) loadLocation()
    }

    fun retryPlaces() {
        val area = _uiState.value.location ?: return
        if (_uiState.value.places is HomePlacesState.Error) loadPlaces(area)
    }

    private fun loadLocation(askPermissionIfFirstTime: Boolean = false, openSettingsIfOff: Boolean = false) {
        locationJob?.cancel()
        _uiState.update { it.copy(location = null) }
        locationJob = viewModelScope.launch {
            val area = getCurrentArea()
            locatedAt = SystemClock.elapsedRealtime()
            _uiState.update { it.copy(location = area) }
            showPlacesFor(area)
            if (area == DeviceArea.PermissionMissing && askPermissionIfFirstTime && claimLocationPrompt()) {
                _events.send(HomeEvent.AskLocationPermission)
            } else if (area == DeviceArea.LocationOff && openSettingsIfOff) {
                _events.send(HomeEvent.OpenLocationSettings)
            }
        }
    }

    /** A list loaded for about the same spot stays; only the texts around it follow the new [area]. */
    private fun showPlacesFor(area: DeviceArea) {
        val shown = _uiState.value.places as? HomePlacesState.Content
        if (shown != null && isSameSpot(placesPoint, area.point)) {
            _uiState.update { it.copy(places = shown.copy(area = area)) }
        } else {
            loadPlaces(area)
        }
    }

    private fun isSameSpot(first: GeoPoint?, second: GeoPoint?): Boolean =
        if (first == null || second == null) first == second else first.distanceKmTo(second) < MOVED_KM

    /** A list already on screen stays while the new one loads, and also when that load fails. */
    private fun loadPlaces(area: DeviceArea) {
        placesJob?.cancel()
        _uiState.update { state ->
            if (state.places is HomePlacesState.Content) state else state.copy(places = HomePlacesState.Loading)
        }
        val point = area.point
        placesJob = viewModelScope.launch {
            try {
                val places = getHomePlaces(point)
                placesPoint = point
                _uiState.update { it.copy(places = HomePlacesState.Content(places, area)) }
            } catch (e: AppError) {
                _uiState.update { state ->
                    if (state.places is HomePlacesState.Content) state else state.copy(places = HomePlacesState.Error(e))
                }
            }
        }
    }

    private val DeviceArea.point: GeoPoint?
        get() = (this as? DeviceArea.Found)?.point

    /**
     * What the review screen opens with; null for a guest, because the backend only suggests to signed-in users.
     * The meal and budget are left to today's saved needs, which the review screen shows and edits.
     */
    fun suggestionArgs(note: String): SuggestionArgs? {
        val state = _uiState.value
        if (!state.isSignedIn) return null
        return SuggestionArgs(
            meal = null,
            budget = null,
            note = note.trim().takeIf { it.isNotEmpty() },
            point = (state.location as? DeviceArea.Found)?.point,
        )
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
                    // The default list: the time of day and today's saved needs. Picks go through the review screen.
                    val result = getSuggestions(SuggestionRequest())
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

    private companion object {
        const val GUEST_DISH_COUNT = 10
        const val SESSION_GAP_MS = 30 * 60 * 1000L
        const val MOVED_KM = 0.5
    }
}
