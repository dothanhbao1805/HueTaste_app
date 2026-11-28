package com.example.huetaste.feature.search

import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.base.common.extension.hideKeyboard
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.openMap
import com.example.huetaste.common.extension.startAiChatActivity
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.common.ui.ListSpacingDecoration
import com.example.huetaste.common.ui.bindNotificationBell
import com.example.huetaste.common.ui.LocationAccess
import com.example.huetaste.common.ui.bindChips
import com.example.huetaste.common.ui.showArea
import com.example.huetaste.common.ui.showAvatar
import com.example.huetaste.databinding.FragmentSearchBinding
import com.example.huetaste.databinding.ItemSearchRecentBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DishSort
import com.example.huetaste.domain.model.PlaceArea
import com.example.huetaste.domain.model.UserProfile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The search tab (Stitch "Tìm Kiếm - HueTaste"): dishes from `GET /mon-an` and places from `GET /dia-diem`, with the
 * filters the backend supports. The ViewModel owns every query and filter, so switching tabs or rotating keeps them.
 */
@AndroidEntryPoint
class SearchFragment : LsFragment<FragmentSearchBinding>(FragmentSearchBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[SearchViewModel::class.java] }

    private val resultAdapter = SearchResultAdapter(
        onDishClick = { item ->
            viewModel.onResultOpened()
            requireActivity().startDishDetailActivity(item.dish.id)
        },
        onDishFavorite = { item -> viewModel.toggleDishFavorite(item.dish.id) },
        onPlaceClick = { item ->
            viewModel.onResultOpened()
            requireActivity().startPlaceDetailActivity(item.place.id)
        },
        onPlaceFavorite = { item -> viewModel.togglePlaceFavorite(item.place.id) },
        onDirections = { item -> requireContext().openMap(item.place) },
    )

    private val locationAccess = LocationAccess(
        fragment = this,
        onPermissionResult = { granted -> viewModel.refreshLocation(openSettingsIfOff = granted) },
        onSettingsClosed = { viewModel.refreshLocation() },
    )

    private val loadMoreListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
            if (dy > 0 && layout.findLastVisibleItemPosition() >= resultAdapter.itemCount - LOAD_MORE_AHEAD) {
                viewModel.loadMore()
            }
        }
    }

    /** What the empty or error block's buttons do right now. */
    private var primaryAction: (() -> Unit)? = null
    private var secondaryAction: (() -> Unit)? = null

    private var shownUser: UserProfile? = null
    private var shownRecent: List<String>? = null

    override fun onViewCreated() {
        with(binding) {
            resultList.layoutManager = LinearLayoutManager(requireContext())
            resultList.adapter = resultAdapter
            resultList.addItemDecoration(ListSpacingDecoration(1, resources.getDimensionPixelSize(R.dimen._12dp)))
            resultList.addOnScrollListener(loadMoreListener)

            header.locationGroup.clicks {
                locationAccess.onAreaClicked(viewModel.uiState.value.location) { viewModel.refreshLocation() }
            }
            bindNotificationBell(header)

            searchInput.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
            searchInput.setOnEditorActionListener { view, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
                view.clearFocus()
                hideKeyboard()
                viewModel.submit()
                true
            }
            searchClear.clicks { searchInput.setText("") }

            tabDishes.clicks { viewModel.selectTab(SearchTab.DISHES) }
            tabPlaces.clicks { viewModel.selectTab(SearchTab.PLACES) }
            ingredientChip.clicks {
                IngredientFilterSheet.show(childFragmentManager, viewModel.uiState.value.filters.ingredients)
            }
            nearMeChip.clicks { viewModel.toggleNearMe() }
            radiusChip.clicks { RadiusSheet.show(childFragmentManager, viewModel.uiState.value.filters.radiusKm) }
            sortButton.clicks { showSortMenu() }

            recentClear.clicks { viewModel.clearRecentSearches() }
            aiSuggestButton.clicks { requireContext().startAiChatActivity() }
            suggestionGroup.bindChips(
                resources.getStringArray(R.array.search_suggestions).toList(),
                { it },
                { it },
                { false },
            ) { term -> searchFor(term) }
            messagePrimary.clicks { primaryAction?.invoke() }
            messageSecondary.clicks { secondaryAction?.invoke() }
        }

        childFragmentManager.setFragmentResultListener(IngredientFilterSheet.RESULT_KEY, viewLifecycleOwner) { _, result ->
            viewModel.setIngredients(result.getStringArrayList(IngredientFilterSheet.RESULT_INGREDIENTS).orEmpty())
        }
        childFragmentManager.setFragmentResultListener(RadiusSheet.RESULT_KEY, viewLifecycleOwner) { _, result ->
            viewModel.setRadius(result.getInt(RadiusSheet.RESULT_RADIUS, SearchFilters.DEFAULT_RADIUS_KM))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // Hearts change on other screens, so every return to this tab reads them again.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refreshFavorites() }
        }
    }

    private fun searchFor(term: String) {
        binding.searchInput.clearFocus()
        hideKeyboard()
        viewModel.searchFor(term)
    }

    private fun onEvent(event: SearchEvent) {
        when (event) {
            SearchEvent.AskLocationPermission -> locationAccess.requestPermission()
            SearchEvent.OpenLocationSettings -> locationAccess.openLocationSettings()
            SearchEvent.LocationUnavailable -> requireContext().makeToast(R.string.search_location_unavailable)
            SearchEvent.LoadMoreFailed -> requireContext().makeToast(R.string.search_load_more_failed)
            SearchEvent.FavoriteFailed -> requireContext().makeToast(R.string.search_favorite_failed)
        }
    }

    private fun render(state: SearchUiState) = with(binding) {
        header.showArea(state.location, R.string.search_header_subtitle)
        if (state.user != null && state.user != shownUser) {
            shownUser = state.user
            header.showAvatar(state.user.avatarUrl)
        }

        // Typing changes the state first, so only an outside change (a suggestion, a restore) rewrites the box.
        if (searchInput.text?.toString() != state.query) {
            searchInput.setText(state.query)
            searchInput.setSelection(state.query.length)
        }
        searchClear.isVisible = state.query.isNotEmpty()

        val onDishes = state.tab == SearchTab.DISHES
        tabDishes.isSelected = onDishes
        tabPlaces.isSelected = !onDishes
        dishFilterRow.isVisible = onDishes
        placeFilterRow.isVisible = !onDishes
        showFilters(state)

        val content = state.content
        resultBar.isVisible = content == SearchContent.Results
        resultCount.text = getString(R.string.search_result_count, state.total)
        sortButton.isVisible = onDishes
        sortButton.text = getString(R.string.search_sort_label, getString(state.filters.sort.label))

        resultList.isVisible = content == SearchContent.Results
        stateScroll.isVisible = content != SearchContent.Results
        initialBlock.isVisible = content == SearchContent.Initial
        skeletonBlock.isVisible = content == SearchContent.Loading
        messageBlock.isVisible = content == SearchContent.Empty || content is SearchContent.Error
        when (content) {
            SearchContent.Initial -> showRecent(state.recent)
            SearchContent.Empty -> showEmpty()
            is SearchContent.Error -> showError(content.error)
            else -> Unit
        }

        resultAdapter.submitList(resultItems(state))
    }

    private fun showFilters(state: SearchUiState) = with(binding) {
        val filters = state.filters
        val categories = listOf(null to getString(R.string.search_filter_all)) +
            state.categories.map { it.id to it.name }
        categoryGroup.bindChips(categories, { it.first ?: ALL_KEY }, { it.second }, { it.first == filters.categoryId }) {
            viewModel.selectCategory(it.first)
        }
        ingredientChip.isSelected = filters.ingredients.isNotEmpty()
        ingredientChip.text = if (filters.ingredients.isEmpty()) {
            getString(R.string.search_ingredient_chip)
        } else {
            getString(R.string.search_ingredient_chip_count, filters.ingredients.size)
        }

        nearMeChip.isSelected = filters.nearMe
        radiusChip.text = getString(R.string.search_radius_chip, filters.radiusKm)
        val areas = listOf<PlaceArea?>(null) + PlaceArea.entries
        areaGroup.bindChips(areas, { it?.name ?: ALL_KEY }, { getString(it.label) }, { it == filters.area }) {
            viewModel.selectArea(it)
        }
    }

    /** The recent searches are rebuilt only when the list changes; the card hides when there are none. */
    private fun showRecent(recent: List<String>) = with(binding) {
        recentCard.isVisible = recent.isNotEmpty()
        if (recent == shownRecent) return@with
        shownRecent = recent
        recentList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        recent.forEach { term ->
            val row = ItemSearchRecentBinding.inflate(inflater, recentList, false)
            row.recentTerm.text = term
            row.recentTerm.clicks { searchFor(term) }
            row.recentRemove.contentDescription = getString(R.string.search_recent_remove_cd, term)
            row.recentRemove.clicks { viewModel.removeRecentSearch(term) }
            recentList.addView(row.root)
        }
    }

    private fun showEmpty() {
        val suggestion = resources.getStringArray(R.array.search_suggestions).first()
        showMessage(
            icon = R.drawable.ic_soup_kitchen,
            title = R.string.search_empty_title,
            message = getString(R.string.search_empty_message),
            primary = getString(R.string.search_clear_filters),
            primaryIcon = R.drawable.ic_restart_alt,
            onPrimary = { viewModel.clearFilters() },
            secondary = getString(R.string.search_try_term, suggestion),
            onSecondary = { searchFor(suggestion) },
        )
    }

    private fun showError(error: AppError) {
        showMessage(
            icon = R.drawable.ic_cloud_off,
            title = R.string.search_error_title,
            message = getString(
                if (error.code == AppError.NETWORK) R.string.ai_chat_error_network else R.string.search_error_message,
            ),
            primary = getString(R.string.search_retry),
            primaryIcon = R.drawable.ic_refresh,
            onPrimary = { viewModel.retry() },
            secondary = null,
            onSecondary = null,
        )
    }

    @Suppress("LongParameterList")
    private fun showMessage(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        message: String,
        primary: String,
        @DrawableRes primaryIcon: Int,
        onPrimary: () -> Unit,
        secondary: String?,
        onSecondary: (() -> Unit)?,
    ) = with(binding) {
        messageIcon.setImageResource(icon)
        messageTitle.setText(title)
        messageText.text = message
        messagePrimary.text = primary
        messagePrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(primaryIcon, 0, 0, 0)
        primaryAction = onPrimary
        messageSecondary.isVisible = secondary != null
        messageSecondary.text = secondary
        secondaryAction = onSecondary
    }

    private fun resultItems(state: SearchUiState): List<SearchResultItem> {
        if (state.content != SearchContent.Results) return emptyList()
        val rows: List<SearchResultItem> = when (state.tab) {
            SearchTab.DISHES -> state.dishes.map {
                SearchResultItem.Dish(it, it.dish.id in state.favoriteDishIds, state.isSignedIn)
            }
            SearchTab.PLACES -> state.places.map {
                SearchResultItem.Place(it, it.place.id in state.favoritePlaceIds, state.isSignedIn)
            }
        }
        return if (state.isLoadingMore) rows + SearchResultItem.LoadingMore else rows
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.sortButton)
        DishSort.entries.forEachIndexed { index, sort -> popup.menu.add(0, index, index, sort.label) }
        popup.setOnMenuItemClickListener { item ->
            viewModel.setSort(DishSort.entries[item.itemId])
            true
        }
        popup.show()
    }

    @get:StringRes
    private val DishSort.label
        get() = when (this) {
            DishSort.POPULAR -> R.string.search_sort_popular
            DishSort.NAME -> R.string.search_sort_name
            DishSort.PRICE_LOW -> R.string.search_sort_price
        }

    @get:StringRes
    private val PlaceArea?.label
        get() = when (this) {
            null -> R.string.search_filter_all
            PlaceArea.CO_DO -> R.string.search_area_co_do
            PlaceArea.BO_BAC -> R.string.search_area_bo_bac
            PlaceArea.BO_NAM -> R.string.search_area_bo_nam
        }

    override fun onDestroyView() {
        binding.resultList.removeOnScrollListener(loadMoreListener)
        binding.resultList.adapter = null
        primaryAction = null
        secondaryAction = null
        shownUser = null
        shownRecent = null
        super.onDestroyView()
    }

    private companion object {
        const val LOAD_MORE_AHEAD = 4
        const val ALL_KEY = "all"
    }
}
