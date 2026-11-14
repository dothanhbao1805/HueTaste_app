package com.example.huetaste.feature.favorites

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.Place
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetCurrentLocationUseCase
import com.example.huetaste.domain.usecase.GetDishCategoriesUseCase
import com.example.huetaste.domain.usecase.GetFavoritesUseCase
import com.example.huetaste.domain.usecase.SetDishFavoriteUseCase
import com.example.huetaste.domain.usecase.SetPlaceFavoriteUseCase
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
import kotlin.random.Random

sealed interface FavoritesLoad {
    /** Nothing is saved for a guest, so nothing is asked of the backend. */
    object Guest : FavoritesLoad

    /** The first load; a reload keeps showing the lists already on screen. */
    object Loading : FavoritesLoad
    data class Error(val error: AppError) : FavoritesLoad
    object Loaded : FavoritesLoad
}

data class FavoritesUiState(
    val load: FavoritesLoad = FavoritesLoad.Loading,
    val dishes: List<Dish> = emptyList(),
    val places: List<Place> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    /** Null while unknown or when the device gave no position; the nearest-first sort is then hidden. */
    val position: GeoPoint? = null,
    val tab: FavoritesTab = FavoritesTab.DISHES,
    val mealFilter: MealFilter = MealFilter.ALL,
    val placeFilter: PlaceFilter = PlaceFilter.ALL,
    val dishSort: DishSort = DishSort.RECENT,
    val placeSort: PlaceSort = PlaceSort.RECENT,
    val query: String = "",
) {
    val dishItems: List<FavoriteDishItem> by lazy {
        FavoritesFilter.dishes(dishes, categoryNames, mealFilter, dishSort, query)
    }
    val placeItems: List<FavoritePlaceItem> by lazy {
        FavoritesFilter.places(places, position, placeFilter, placeSort, query)
    }

    /** The automatic collections of the tab on screen, whatever the filters say. */
    val collections: List<CollectionSummary> by lazy {
        FavoriteCollections.summaries(tab == FavoritesTab.DISHES, dishes, places, position)
    }

    /** Places open right now among everything saved, whatever the filters say. */
    val openCount: Int by lazy { FavoritesFilter.places(places, null, PlaceFilter.OPEN_NOW, PlaceSort.RECENT, "").size }

    val isFiltering: Boolean
        get() = query.isNotBlank() || when (tab) {
            FavoritesTab.DISHES -> mealFilter != MealFilter.ALL
            FavoritesTab.PLACES -> placeFilter != PlaceFilter.ALL
        }
}

sealed interface FavoritesEvent {
    /** A heart was removed; the screen offers to undo it. */
    data class Removed(val name: String) : FavoritesEvent
    object ActionFailed : FavoritesEvent

    /** "Chọn ngẫu nhiên" chose this saved dish; the screen shows its quick view. */
    data class Picked(val dishId: String) : FavoritesEvent
}

/**
 * Loads every saved dish and place in one request and filters and sorts them here, since the backend has no
 * paging or filtering for favorites. The screen reloads whenever it comes back into view, because hearts can
 * change on the home and detail screens.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getAuthState: GetAuthStateUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val getCategories: GetDishCategoriesUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val setDishFavorite: SetDishFavoriteUseCase,
    private val setPlaceFavorite: SetPlaceFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoredState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var locationJob: Job? = null

    /** When the position was last read on the clock that keeps running in sleep; 0 means never. */
    private var locatedAt = 0L

    /** Ids whose removal is still on its way to the backend, so a reload meanwhile does not bring them back. */
    private val pendingDishIds = mutableSetOf<String>()
    private val pendingPlaceIds = mutableSetOf<String>()

    private var lastRemoved: Removed? = null
    private var lastPickedId: String? = null

    private sealed interface Removed {
        data class OfDish(val dish: Dish, val index: Int) : Removed
        data class OfPlace(val place: Place, val index: Int) : Removed
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (getAuthState() != AuthState.SIGNED_IN) {
                _uiState.update { it.copy(load = FavoritesLoad.Guest, dishes = emptyList(), places = emptyList()) }
                return@launch
            }
            _uiState.update { state ->
                if (state.load == FavoritesLoad.Loaded) state else state.copy(load = FavoritesLoad.Loading)
            }
            loadCategoriesIfMissing()
            loadPositionIfStale()
            try {
                val favorites = getFavorites()
                _uiState.update {
                    it.copy(
                        load = FavoritesLoad.Loaded,
                        dishes = favorites.dishes.filter { dish -> dish.id !in pendingDishIds },
                        places = favorites.places.filter { place -> place.id !in pendingPlaceIds },
                    )
                }
            } catch (e: AppError) {
                // A list already on screen stays; only the first load shows the error.
                _uiState.update { state ->
                    if (state.load == FavoritesLoad.Loaded) state else state.copy(load = FavoritesLoad.Error(e))
                }
            }
        }
    }

    fun selectTab(tab: FavoritesTab) {
        savedState[KEY_TAB] = tab.name
        _uiState.update { it.copy(tab = tab) }
    }

    fun selectMealFilter(filter: MealFilter) {
        savedState[KEY_MEAL] = filter.name
        _uiState.update { it.copy(mealFilter = filter) }
    }

    fun selectPlaceFilter(filter: PlaceFilter) {
        savedState[KEY_PLACE_FILTER] = filter.name
        _uiState.update { it.copy(placeFilter = filter) }
    }

    fun selectDishSort(sort: DishSort) {
        savedState[KEY_DISH_SORT] = sort.name
        _uiState.update { it.copy(dishSort = sort) }
    }

    fun selectPlaceSort(sort: PlaceSort) {
        savedState[KEY_PLACE_SORT] = sort.name
        _uiState.update { it.copy(placeSort = sort) }
    }

    fun search(query: String) {
        savedState[KEY_QUERY] = query
        _uiState.update { it.copy(query = query) }
    }

    fun clearFilters() {
        search("")
        selectMealFilter(MealFilter.ALL)
        selectPlaceFilter(PlaceFilter.ALL)
    }

    /** Picks a saved dish that fits the current meal and asks the screen to show it. */
    fun pickRandomDish() {
        val mealCode = FavoritesFilter.mealCodeForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        val dish = FavoritesFilter.pickRandom(_uiState.value.dishes, mealCode, lastPickedId, Random.Default) ?: return
        lastPickedId = dish.id
        viewModelScope.launch { _events.send(FavoritesEvent.Picked(dish.id)) }
    }

    /** Gone from the list at once and put back if the backend refuses. */
    fun removeDish(dishId: String) {
        val dishes = _uiState.value.dishes
        val index = dishes.indexOfFirst { it.id == dishId }
        if (index < 0) return
        val dish = dishes[index]
        lastRemoved = Removed.OfDish(dish, index)
        pendingDishIds += dishId
        _uiState.update { it.copy(dishes = it.dishes.filterNot { item -> item.id == dishId }) }
        viewModelScope.launch {
            try {
                setDishFavorite(dishId, false)
                _events.send(FavoritesEvent.Removed(dish.name))
            } catch (e: AppError) {
                if (e.code == AppError.FAVORITE_NOT_FOUND) {
                    _events.send(FavoritesEvent.Removed(dish.name))
                } else {
                    lastRemoved = null
                    insertDish(dish, index)
                    _events.send(FavoritesEvent.ActionFailed)
                }
            } finally {
                pendingDishIds -= dishId
            }
        }
    }

    fun removePlace(placeId: String) {
        val places = _uiState.value.places
        val index = places.indexOfFirst { it.id == placeId }
        if (index < 0) return
        val place = places[index]
        lastRemoved = Removed.OfPlace(place, index)
        pendingPlaceIds += placeId
        _uiState.update { it.copy(places = it.places.filterNot { item -> item.id == placeId }) }
        viewModelScope.launch {
            try {
                setPlaceFavorite(placeId, false)
                _events.send(FavoritesEvent.Removed(place.name))
            } catch (e: AppError) {
                if (e.code == AppError.FAVORITE_NOT_FOUND) {
                    _events.send(FavoritesEvent.Removed(place.name))
                } else {
                    lastRemoved = null
                    insertPlace(place, index)
                    _events.send(FavoritesEvent.ActionFailed)
                }
            } finally {
                pendingPlaceIds -= placeId
            }
        }
    }

    /** Saves the last removed heart again and puts the card back where it was. */
    fun undoRemove() {
        val removed = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch {
            when (removed) {
                is Removed.OfDish -> {
                    insertDish(removed.dish, removed.index)
                    try {
                        setDishFavorite(removed.dish.id, true)
                    } catch (e: AppError) {
                        _uiState.update { it.copy(dishes = it.dishes.filterNot { dish -> dish.id == removed.dish.id }) }
                        _events.send(FavoritesEvent.ActionFailed)
                    }
                }
                is Removed.OfPlace -> {
                    insertPlace(removed.place, removed.index)
                    try {
                        setPlaceFavorite(removed.place.id, true)
                    } catch (e: AppError) {
                        _uiState.update { it.copy(places = it.places.filterNot { place -> place.id == removed.place.id }) }
                        _events.send(FavoritesEvent.ActionFailed)
                    }
                }
            }
        }
    }

    private fun insertDish(dish: Dish, index: Int) {
        _uiState.update { state ->
            if (state.dishes.any { it.id == dish.id }) {
                state
            } else {
                state.copy(dishes = state.dishes.toMutableList().apply { add(index.coerceIn(0, size), dish) })
            }
        }
    }

    private fun insertPlace(place: Place, index: Int) {
        _uiState.update { state ->
            if (state.places.any { it.id == place.id }) {
                state
            } else {
                state.copy(places = state.places.toMutableList().apply { add(index.coerceIn(0, size), place) })
            }
        }
    }

    /** The names are only for the category line of a card, so a failure just leaves that line out. */
    private suspend fun loadCategoriesIfMissing() {
        if (_uiState.value.categoryNames.isNotEmpty()) return
        try {
            val names = getCategories().associate { it.id to it.name }
            _uiState.update { it.copy(categoryNames = names) }
        } catch (e: AppError) {
            // Cards show no category until a later reload succeeds.
        }
    }

    /** Read in the background: the position can take seconds and only the nearest-first sort needs it. */
    private fun loadPositionIfStale() {
        val fresh = locatedAt != 0L && SystemClock.elapsedRealtime() - locatedAt < POSITION_MAX_AGE_MS
        if (fresh || locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            val point = getCurrentLocation()
            locatedAt = SystemClock.elapsedRealtime()
            _uiState.update { state ->
                state.copy(
                    position = point,
                    placeSort = if (point == null && state.placeSort == PlaceSort.NEAREST) PlaceSort.RECENT else state.placeSort,
                )
            }
        }
    }

    private fun restoredState() = FavoritesUiState(
        tab = restored(KEY_TAB, FavoritesTab.DISHES),
        mealFilter = restored(KEY_MEAL, MealFilter.ALL),
        placeFilter = restored(KEY_PLACE_FILTER, PlaceFilter.ALL),
        dishSort = restored(KEY_DISH_SORT, DishSort.RECENT),
        placeSort = restored(KEY_PLACE_SORT, PlaceSort.RECENT),
        query = savedState.get<String>(KEY_QUERY).orEmpty(),
    )

    private inline fun <reified T : Enum<T>> restored(key: String, default: T): T =
        savedState.get<String>(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val KEY_TAB = "favorites_tab"
        const val KEY_MEAL = "favorites_meal_filter"
        const val KEY_PLACE_FILTER = "favorites_place_filter"
        const val KEY_DISH_SORT = "favorites_dish_sort"
        const val KEY_PLACE_SORT = "favorites_place_sort"
        const val KEY_QUERY = "favorites_query"
        const val POSITION_MAX_AGE_MS = 5 * 60 * 1000L
    }
}
