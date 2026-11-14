package com.example.huetaste.feature.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.Place
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
import javax.inject.Inject

data class FavoriteCollectionUiState(
    val collection: FavoriteCollection,
    val load: FavoritesLoad = FavoritesLoad.Loading,
    val dishes: List<Dish> = emptyList(),
    val places: List<Place> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    val position: GeoPoint? = null,
    val dishSort: DishSort = DishSort.RECENT,
    val placeSort: PlaceSort = PlaceSort.RECENT,
) {
    val dishItems: List<FavoriteDishItem> by lazy {
        FavoritesFilter.dishes(FavoriteCollections.dishes(collection, dishes), categoryNames, MealFilter.ALL, dishSort, "")
    }
    val placeItems: List<FavoritePlaceItem> by lazy {
        FavoriteCollections.places(collection, places, position, placeSort)
    }
    val itemCount: Int get() = if (collection.isDishes) dishItems.size else placeItems.size
}

/**
 * The items of one automatic collection. It loads the favorites itself (the same request as the Favorites tab) and
 * applies the collection rule, so what is shown always matches the card that opened it.
 */
@HiltViewModel
class FavoriteCollectionViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getFavorites: GetFavoritesUseCase,
    private val getCategories: GetDishCategoriesUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val setDishFavorite: SetDishFavoriteUseCase,
    private val setPlaceFavorite: SetPlaceFavoriteUseCase,
) : ViewModel() {

    private val collection: FavoriteCollection =
        savedState.get<String>(FavoriteCollectionActivity.EXTRA_COLLECTION)
            ?.let { name -> FavoriteCollection.entries.firstOrNull { it.name == name } }
            ?: FavoriteCollection.MORNING_DISHES

    private val _uiState = MutableStateFlow(
        FavoriteCollectionUiState(
            collection = collection,
            dishSort = restored(KEY_DISH_SORT, DishSort.RECENT),
            // The "near you" collection starts with the nearest place.
            placeSort = restored(KEY_PLACE_SORT, if (collection == FavoriteCollection.NEARBY_PLACES) PlaceSort.NEAREST else PlaceSort.RECENT),
        ),
    )
    val uiState: StateFlow<FavoriteCollectionUiState> = _uiState.asStateFlow()

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var lastRemovedDish: Pair<Dish, Int>? = null
    private var lastRemovedPlace: Pair<Place, Int>? = null
    private val pendingDishIds = mutableSetOf<String>()
    private val pendingPlaceIds = mutableSetOf<String>()

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { if (it.load == FavoritesLoad.Loaded) it else it.copy(load = FavoritesLoad.Loading) }
            if (_uiState.value.categoryNames.isEmpty() && collection.isDishes) {
                try {
                    val names = getCategories().associate { it.id to it.name }
                    _uiState.update { it.copy(categoryNames = names) }
                } catch (e: AppError) {
                    // Cards show no category line.
                }
            }
            if (!collection.isDishes && _uiState.value.position == null) {
                val point = getCurrentLocation()
                _uiState.update { state ->
                    state.copy(
                        position = point,
                        placeSort = if (point == null && state.placeSort == PlaceSort.NEAREST) PlaceSort.RECENT else state.placeSort,
                    )
                }
            }
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
                _uiState.update { if (it.load == FavoritesLoad.Loaded) it else it.copy(load = FavoritesLoad.Error(e)) }
            }
        }
    }

    fun selectDishSort(sort: DishSort) {
        savedState[KEY_DISH_SORT] = sort.name
        _uiState.update { it.copy(dishSort = sort) }
    }

    fun selectPlaceSort(sort: PlaceSort) {
        savedState[KEY_PLACE_SORT] = sort.name
        _uiState.update { it.copy(placeSort = sort) }
    }

    fun removeDish(dishId: String) {
        val index = _uiState.value.dishes.indexOfFirst { it.id == dishId }
        if (index < 0) return
        val dish = _uiState.value.dishes[index]
        lastRemovedDish = dish to index
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
                    lastRemovedDish = null
                    insertDish(dish, index)
                    _events.send(FavoritesEvent.ActionFailed)
                }
            } finally {
                pendingDishIds -= dishId
            }
        }
    }

    fun removePlace(placeId: String) {
        val index = _uiState.value.places.indexOfFirst { it.id == placeId }
        if (index < 0) return
        val place = _uiState.value.places[index]
        lastRemovedPlace = place to index
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
                    lastRemovedPlace = null
                    insertPlace(place, index)
                    _events.send(FavoritesEvent.ActionFailed)
                }
            } finally {
                pendingPlaceIds -= placeId
            }
        }
    }

    /** Saves the last removed heart again and puts the card back. */
    fun undoRemove() {
        val dish = lastRemovedDish
        val place = lastRemovedPlace
        lastRemovedDish = null
        lastRemovedPlace = null
        viewModelScope.launch {
            if (dish != null) {
                insertDish(dish.first, dish.second)
                try {
                    setDishFavorite(dish.first.id, true)
                } catch (e: AppError) {
                    _uiState.update { it.copy(dishes = it.dishes.filterNot { item -> item.id == dish.first.id }) }
                    _events.send(FavoritesEvent.ActionFailed)
                }
            } else if (place != null) {
                insertPlace(place.first, place.second)
                try {
                    setPlaceFavorite(place.first.id, true)
                } catch (e: AppError) {
                    _uiState.update { it.copy(places = it.places.filterNot { item -> item.id == place.first.id }) }
                    _events.send(FavoritesEvent.ActionFailed)
                }
            }
        }
    }

    private fun insertDish(dish: Dish, index: Int) {
        _uiState.update { state ->
            if (state.dishes.any { it.id == dish.id }) state
            else state.copy(dishes = state.dishes.toMutableList().apply { add(index.coerceIn(0, size), dish) })
        }
    }

    private fun insertPlace(place: Place, index: Int) {
        _uiState.update { state ->
            if (state.places.any { it.id == place.id }) state
            else state.copy(places = state.places.toMutableList().apply { add(index.coerceIn(0, size), place) })
        }
    }

    private inline fun <reified T : Enum<T>> restored(key: String, default: T): T =
        savedState.get<String>(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val KEY_DISH_SORT = "collection_dish_sort"
        const val KEY_PLACE_SORT = "collection_place_sort"
    }
}
