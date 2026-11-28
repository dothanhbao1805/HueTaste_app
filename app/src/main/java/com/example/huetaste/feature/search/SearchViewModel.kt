package com.example.huetaste.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.model.DishSearchItem
import com.example.huetaste.domain.model.DishSearchQuery
import com.example.huetaste.domain.model.DishSort
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.PlaceArea
import com.example.huetaste.domain.model.PlaceSearchItem
import com.example.huetaste.domain.model.PlaceSearchQuery
import com.example.huetaste.domain.model.SearchPage
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.usecase.AddRecentSearchUseCase
import com.example.huetaste.domain.usecase.ClearRecentSearchesUseCase
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetCurrentAreaUseCase
import com.example.huetaste.domain.usecase.GetCurrentUserUseCase
import com.example.huetaste.domain.usecase.GetDishCategoriesUseCase
import com.example.huetaste.domain.usecase.GetFavoritesUseCase
import com.example.huetaste.domain.usecase.GetRecentSearchesUseCase
import com.example.huetaste.domain.usecase.RemoveRecentSearchUseCase
import com.example.huetaste.domain.usecase.SearchDishesUseCase
import com.example.huetaste.domain.usecase.SearchPlacesUseCase
import com.example.huetaste.domain.usecase.SetDishFavoriteUseCase
import com.example.huetaste.domain.usecase.SetPlaceFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab { DISHES, PLACES }

data class SearchFilters(
    val categoryId: String? = null,
    val ingredients: List<String> = emptyList(),
    val sort: DishSort = DishSort.POPULAR,
    val nearMe: Boolean = false,
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val area: PlaceArea? = null,
) {
    /** The sort and the radius alone do not start a search; they only shape one. */
    fun narrows(tab: SearchTab): Boolean = when (tab) {
        SearchTab.DISHES -> categoryId != null || ingredients.isNotEmpty()
        SearchTab.PLACES -> nearMe || area != null
    }

    companion object {
        const val DEFAULT_RADIUS_KM = 5
        const val MAX_RADIUS_KM = 50
    }
}

sealed interface SearchContent {
    /** Nothing typed and no filter: recent searches and suggestions. */
    object Initial : SearchContent
    object Loading : SearchContent
    object Results : SearchContent
    object Empty : SearchContent
    data class Error(val error: AppError) : SearchContent
}

data class SearchUiState(
    val tab: SearchTab = SearchTab.DISHES,
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val categories: List<DishCategory> = emptyList(),
    val recent: List<String> = emptyList(),
    val content: SearchContent = SearchContent.Initial,
    val dishes: List<DishSearchItem> = emptyList(),
    val places: List<PlaceSearchItem> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** null while the device is being located. */
    val location: DeviceArea? = null,
    val user: UserProfile? = null,
    val isSignedIn: Boolean = false,
    val favoriteDishIds: Set<String> = emptySet(),
    val favoritePlaceIds: Set<String> = emptySet(),
)

sealed interface SearchEvent {
    object AskLocationPermission : SearchEvent
    object OpenLocationSettings : SearchEvent
    /** "Gần tôi" was asked for but no position could be read. */
    object LocationUnavailable : SearchEvent
    object LoadMoreFailed : SearchEvent
    object FavoriteFailed : SearchEvent
}

/**
 * Searches `GET /mon-an` and `GET /dia-diem` page by page. Typing waits [TYPING_DEBOUNCE_MS] before asking; every new
 * query or filter cancels the request in flight, so a slow old answer never replaces a newer one.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val searchDishes: SearchDishesUseCase,
    private val searchPlaces: SearchPlacesUseCase,
    private val getCategories: GetDishCategoriesUseCase,
    private val getRecent: GetRecentSearchesUseCase,
    private val addRecent: AddRecentSearchUseCase,
    private val removeRecent: RemoveRecentSearchUseCase,
    private val clearRecent: ClearRecentSearchesUseCase,
    private val getCurrentArea: GetCurrentAreaUseCase,
    private val getAuthState: GetAuthStateUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val setDishFavorite: SetDishFavoriteUseCase,
    private val setPlaceFavorite: SetPlaceFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoredState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var searchJob: Job? = null
    private var locationJob: Job? = null
    private var nextPage = 1

    /** "Gần tôi" waits for a position; [askedForNearMe] keeps the permission or settings step to one try. */
    private var wantsNearMe = savedState.get<Boolean>(KEY_NEAR_ME) == true
    private var askedForNearMe = false

    init {
        loadLocation()
        viewModelScope.launch {
            runCatching { getRecent() }.onSuccess { recent -> _uiState.update { it.copy(recent = recent) } }
        }
        loadCategories()
        viewModelScope.launch {
            val signedIn = getAuthState() == AuthState.SIGNED_IN
            _uiState.update { it.copy(isSignedIn = signedIn) }
        }
        viewModelScope.launch {
            getCurrentUser.observe().filterNotNull().collect { user -> _uiState.update { it.copy(user = user) } }
        }
        // A restored "Gần tôi" waits for the position; anything else can search right away.
        if (!wantsNearMe) search(debounce = false)
    }

    fun setQuery(text: String) {
        if (text == _uiState.value.query) return
        savedState[KEY_QUERY] = text
        _uiState.update { it.copy(query = text) }
        search(debounce = true)
    }

    /** The keyboard's search key: asks now and remembers the term. */
    fun submit() {
        rememberQuery()
        search(debounce = false)
    }

    /** A recent search or a suggestion. */
    fun searchFor(term: String) {
        savedState[KEY_QUERY] = term
        _uiState.update { it.copy(query = term) }
        submit()
    }

    fun selectTab(tab: SearchTab) {
        if (tab == _uiState.value.tab) return
        savedState[KEY_TAB] = tab.name
        _uiState.update { it.copy(tab = tab, dishes = emptyList(), places = emptyList()) }
        search(debounce = false)
    }

    fun selectCategory(categoryId: String?) = updateFilters { it.copy(categoryId = categoryId) }

    fun setIngredients(names: List<String>) = updateFilters { it.copy(ingredients = names) }

    fun setSort(sort: DishSort) = updateFilters { it.copy(sort = sort) }

    fun setRadius(km: Int) = updateFilters { it.copy(radiusKm = km.coerceIn(1, SearchFilters.MAX_RADIUS_KM)) }

    fun selectArea(area: PlaceArea?) = updateFilters { it.copy(area = area) }

    fun toggleNearMe() {
        if (_uiState.value.filters.nearMe || wantsNearMe) {
            wantsNearMe = false
            updateFilters { it.copy(nearMe = false) }
            return
        }
        wantsNearMe = true
        askedForNearMe = false
        val area = _uiState.value.location ?: return // still locating: the arriving area finishes the job
        viewModelScope.launch { onArea(area, settingsOpening = false) }
    }

    /** Clears the filters of the tab on screen and keeps the other tab's. */
    fun clearFilters() {
        val tab = _uiState.value.tab
        wantsNearMe = false
        updateFilters { filters ->
            when (tab) {
                SearchTab.DISHES -> filters.copy(categoryId = null, ingredients = emptyList())
                SearchTab.PLACES -> filters.copy(nearMe = false, area = null)
            }
        }
    }

    fun retry() = search(debounce = false)

    /** @param openSettingsIfOff true right after the permission was granted, when device location is the last step. */
    fun refreshLocation(openSettingsIfOff: Boolean = false) = loadLocation(openSettingsIfOff)

    /** Each time the tab comes back: a dish or place may have been saved or removed on another screen. */
    fun refreshFavorites() {
        viewModelScope.launch {
            if (getAuthState() == AuthState.SIGNED_IN) loadFavorites()
        }
    }

    /** Remembers what is typed when a result is opened. */
    fun onResultOpened() = rememberQuery()

    fun removeRecentSearch(term: String) {
        viewModelScope.launch {
            removeRecent(term)
            _uiState.update { it.copy(recent = getRecent()) }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            clearRecent()
            _uiState.update { it.copy(recent = emptyList()) }
        }
    }

    /** Called when the list nears its end. */
    fun loadMore() {
        val state = _uiState.value
        if (state.content != SearchContent.Results || state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        val request = currentRequest()
        searchJob = viewModelScope.launch {
            try {
                val page = request.load(nextPage)
                nextPage++
                _uiState.update { current -> current.withPage(page, append = true) }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _events.send(SearchEvent.LoadMoreFailed)
            }
        }
    }

    /** Applied at once and undone if the backend refuses. */
    fun toggleDishFavorite(dishId: String) {
        val favorite = dishId !in _uiState.value.favoriteDishIds
        showDishFavorite(dishId, favorite)
        viewModelScope.launch {
            try {
                setDishFavorite(dishId, favorite)
            } catch (e: AppError) {
                showDishFavorite(dishId, !favorite)
                _events.send(SearchEvent.FavoriteFailed)
            }
        }
    }

    fun togglePlaceFavorite(placeId: String) {
        val favorite = placeId !in _uiState.value.favoritePlaceIds
        showPlaceFavorite(placeId, favorite)
        viewModelScope.launch {
            try {
                setPlaceFavorite(placeId, favorite)
            } catch (e: AppError) {
                showPlaceFavorite(placeId, !favorite)
                _events.send(SearchEvent.FavoriteFailed)
            }
        }
    }

    private fun showDishFavorite(id: String, favorite: Boolean) = _uiState.update {
        it.copy(favoriteDishIds = if (favorite) it.favoriteDishIds + id else it.favoriteDishIds - id)
    }

    private fun showPlaceFavorite(id: String, favorite: Boolean) = _uiState.update {
        it.copy(favoritePlaceIds = if (favorite) it.favoritePlaceIds + id else it.favoritePlaceIds - id)
    }

    private fun updateFilters(change: (SearchFilters) -> SearchFilters) {
        val filters = change(_uiState.value.filters)
        if (filters == _uiState.value.filters) return
        saveFilters(filters)
        _uiState.update { it.copy(filters = filters) }
        search(debounce = false)
    }

    private fun search(debounce: Boolean) {
        searchJob?.cancel()
        val state = _uiState.value
        if (state.query.isBlank() && !state.filters.narrows(state.tab)) {
            _uiState.update {
                it.copy(content = SearchContent.Initial, dishes = emptyList(), places = emptyList(), hasMore = false)
            }
            return
        }
        val request = currentRequest()
        searchJob = viewModelScope.launch {
            if (debounce) delay(TYPING_DEBOUNCE_MS)
            _uiState.update { it.copy(content = SearchContent.Loading, isLoadingMore = false) }
            try {
                val page = request.load(1)
                nextPage = 2
                _uiState.update { it.withPage(page, append = false) }
            } catch (e: AppError) {
                _uiState.update { it.copy(content = SearchContent.Error(e), isLoadingMore = false) }
            }
        }
    }

    /** What the screen shows now, fixed when the request starts so a later change cannot mix into its pages. */
    private fun currentRequest(): Request {
        val state = _uiState.value
        val filters = state.filters
        return when (state.tab) {
            SearchTab.DISHES -> Request.Dishes(
                DishSearchQuery(state.query, filters.categoryId, filters.ingredients, filters.sort),
            )
            SearchTab.PLACES -> Request.Places(
                PlaceSearchQuery(
                    text = state.query,
                    near = if (filters.nearMe) state.location.point else null,
                    radiusKm = filters.radiusKm.toDouble(),
                    area = filters.area,
                ),
            )
        }
    }

    private suspend fun Request.load(page: Int): SearchPage<*> = when (this) {
        is Request.Dishes -> searchDishes(query, page)
        is Request.Places -> searchPlaces(query, page)
    }

    @Suppress("UNCHECKED_CAST")
    private fun SearchUiState.withPage(page: SearchPage<*>, append: Boolean): SearchUiState {
        val next = when (tab) {
            SearchTab.DISHES -> {
                val items = page.items as List<DishSearchItem>
                val known = if (append) dishes.map { it.dish.id }.toSet() else emptySet()
                copy(dishes = (if (append) dishes else emptyList()) + items.filterNot { it.dish.id in known })
            }
            SearchTab.PLACES -> {
                val items = page.items as List<PlaceSearchItem>
                val known = if (append) places.map { it.place.id }.toSet() else emptySet()
                copy(places = (if (append) places else emptyList()) + items.filterNot { it.place.id in known })
            }
        }
        val isEmpty = if (tab == SearchTab.DISHES) next.dishes.isEmpty() else next.places.isEmpty()
        return next.copy(
            content = if (isEmpty) SearchContent.Empty else SearchContent.Results,
            total = page.total,
            hasMore = page.hasMore,
            isLoadingMore = false,
        )
    }

    private fun loadLocation(openSettingsIfOff: Boolean = false) {
        locationJob?.cancel()
        _uiState.update { it.copy(location = null) }
        locationJob = viewModelScope.launch {
            val area = getCurrentArea()
            val previous = _uiState.value
            _uiState.update { it.copy(location = area) }
            val settingsOpening = area == DeviceArea.LocationOff && openSettingsIfOff
            if (settingsOpening) _events.send(SearchEvent.OpenLocationSettings)
            if (wantsNearMe) {
                onArea(area, settingsOpening)
            } else if (previous.filters.nearMe && previous.tab == SearchTab.PLACES) {
                // The list follows the new position, or drops "Gần tôi" when the position is gone.
                if (area.point == null) updateFilters { it.copy(nearMe = false) } else search(debounce = false)
            }
        }
    }

    /** Turns "Gần tôi" on once a position is known, or takes the user to the one step that is missing. */
    private suspend fun onArea(area: DeviceArea, settingsOpening: Boolean) {
        if (area.point != null) {
            wantsNearMe = false
            updateFilters { it.copy(nearMe = true) }
            return
        }
        if (settingsOpening) return
        // Permission and device location can be fixed by the user once; a known area without a position cannot.
        if (!askedForNearMe && area !is DeviceArea.Found) {
            askedForNearMe = true
            _events.send(
                if (area == DeviceArea.PermissionMissing) SearchEvent.AskLocationPermission
                else SearchEvent.OpenLocationSettings,
            )
        } else {
            wantsNearMe = false
            askedForNearMe = false
            saveFilters(_uiState.value.filters)
            _events.send(SearchEvent.LocationUnavailable)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            // Without categories the dish tab still has "Tất cả" and the ingredient filter.
            runCatching { getCategories() }.onSuccess { list -> _uiState.update { it.copy(categories = list) } }
        }
    }

    private suspend fun loadFavorites() {
        try {
            val favorites = getFavorites()
            _uiState.update { state ->
                state.copy(
                    favoriteDishIds = favorites.dishes.map { it.id }.toSet(),
                    favoritePlaceIds = favorites.places.map { it.id }.toSet(),
                )
            }
        } catch (e: AppError) {
            // The hearts stay empty; a tap still saves the dish or place.
        }
    }

    private fun rememberQuery() {
        val term = _uiState.value.query.trim()
        if (term.isEmpty()) return
        viewModelScope.launch {
            addRecent(term)
            _uiState.update { it.copy(recent = getRecent()) }
        }
    }

    private val DeviceArea?.point: GeoPoint?
        get() = (this as? DeviceArea.Found)?.point

    private fun saveFilters(filters: SearchFilters) {
        savedState[KEY_CATEGORY] = filters.categoryId
        savedState[KEY_INGREDIENTS] = ArrayList(filters.ingredients)
        savedState[KEY_SORT] = filters.sort.name
        savedState[KEY_NEAR_ME] = filters.nearMe || wantsNearMe
        savedState[KEY_RADIUS] = filters.radiusKm
        savedState[KEY_AREA] = filters.area?.name
    }

    /** "Gần tôi" comes back off and waits in [wantsNearMe] until the position is read again. */
    private fun restoredState(): SearchUiState = SearchUiState(
        tab = savedState.get<String>(KEY_TAB)?.let { name -> SearchTab.entries.firstOrNull { it.name == name } }
            ?: SearchTab.DISHES,
        query = savedState.get<String>(KEY_QUERY).orEmpty(),
        filters = SearchFilters(
            categoryId = savedState.get<String>(KEY_CATEGORY),
            ingredients = savedState.get<ArrayList<String>>(KEY_INGREDIENTS).orEmpty(),
            sort = savedState.get<String>(KEY_SORT)?.let { name -> DishSort.entries.firstOrNull { it.name == name } }
                ?: DishSort.POPULAR,
            radiusKm = savedState.get<Int>(KEY_RADIUS) ?: SearchFilters.DEFAULT_RADIUS_KM,
            area = savedState.get<String>(KEY_AREA)?.let { name -> PlaceArea.entries.firstOrNull { it.name == name } },
        ),
    )

    private sealed interface Request {
        data class Dishes(val query: DishSearchQuery) : Request
        data class Places(val query: PlaceSearchQuery) : Request
    }

    private companion object {
        const val TYPING_DEBOUNCE_MS = 350L
        const val KEY_TAB = "search_tab"
        const val KEY_QUERY = "search_query"
        const val KEY_CATEGORY = "search_category"
        const val KEY_INGREDIENTS = "search_ingredients"
        const val KEY_SORT = "search_sort"
        const val KEY_NEAR_ME = "search_near_me"
        const val KEY_RADIUS = "search_radius"
        const val KEY_AREA = "search_area"
    }
}
