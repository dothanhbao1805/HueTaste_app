package com.example.huetaste.feature.favorites

import android.content.ActivityNotFoundException
import android.os.Parcelable
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.animation.DecelerateInterpolator
import android.widget.PopupMenu
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.ui.ListSpacingDecoration
import com.example.huetaste.common.ui.MainHeader
import com.example.huetaste.common.extension.openMap
import com.example.huetaste.common.extension.startDialer
import com.example.huetaste.common.extension.startFavoriteCollectionActivity
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.databinding.FragmentFavoritesBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.feature.main.MainActivity
import com.example.huetaste.feature.main.MainTab
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The saved dishes and places. The backend sends both lists in one request, so the tabs, filters, sorting and
 * search work on the data already loaded; the data is requested again whenever the tab comes back into view.
 */
@AndroidEntryPoint
class FavoritesFragment : LsFragment<FragmentFavoritesBinding>(FragmentFavoritesBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[FavoritesViewModel::class.java] }

    private val dishAdapter = FavoriteDishAdapter(
        onDishClick = { item -> DishQuickViewSheet.show(childFragmentManager, item.dish.id) },
        onRemoveClick = { item -> viewModel.removeDish(item.dish.id) },
    )
    private val collectionAdapter = CollectionAdapter(
        onClick = { summary -> requireActivity().startFavoriteCollectionActivity(summary.collection) },
    )
    private val placeAdapter = FavoritePlaceAdapter(
        onPlaceClick = { item -> requireActivity().startPlaceDetailActivity(item.place.id) },
        onRemoveClick = { item -> viewModel.removePlace(item.place.id) },
        onCallClick = { item -> item.place.phone?.let(::dial) },
        onDirectionsClick = { item -> requireContext().openMap(item.place) },
    )

    private val mealChips
        get() = with(binding) {
            listOf(
                MealFilter.ALL to chipMealAll,
                MealFilter.MORNING to chipMealMorning,
                MealFilter.NOON to chipMealNoon,
                MealFilter.AFTERNOON to chipMealAfternoon,
                MealFilter.EVENING to chipMealEvening,
            )
        }

    /** What the state button of the empty, no-result and error blocks does right now. */
    private var stateAction: (() -> Unit)? = null

    /** The tab the list on screen is built for, so switching tabs swaps the layout manager only once. */
    private var shownTab: FavoritesTab? = null

    /** Each tab keeps its own scroll position while the other one is shown. */
    private val scrollStates = mutableMapOf<FavoritesTab, Parcelable?>()
    private var pendingScrollState: Parcelable? = null

    private var removedSnackbar: Snackbar? = null

    private val mainHeader = MainHeader(this, R.string.favorites_title)

    override fun onViewCreated() {
        mainHeader.bind(binding.header)
        with(binding) {
            tabDishes.clicks { viewModel.selectTab(FavoritesTab.DISHES) }
            tabPlaces.clicks { viewModel.selectTab(FavoritesTab.PLACES) }
            mealChips.forEach { (meal, chip) -> chip.clicks { viewModel.selectMealFilter(meal) } }
            chipPlaceAll.clicks { viewModel.selectPlaceFilter(PlaceFilter.ALL) }
            chipPlaceOpen.clicks { viewModel.selectPlaceFilter(PlaceFilter.OPEN_NOW) }
            sortButton.clicks { showSortMenu() }
            randomButton.clicks { viewModel.pickRandomDish() }
            collectionList.adapter = collectionAdapter
            // The cards are swapped wholesale on a tab change; the fade above replaces the per-item animations.
            collectionList.itemAnimator = null
            stateButton.clicks { stateAction?.invoke() }
            guestLoginButton.clicks { requireActivity().startLoginActivity() }
            searchClear.clicks { searchInput.setText("") }

            searchInput.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
            searchInput.setOnEditorActionListener { view, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    view.clearFocus()
                    requireContext().getSystemService(InputMethodManager::class.java)
                        ?.hideSoftInputFromWindow(view.windowToken, 0)
                    true
                } else {
                    false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Hearts change on other screens, so every return to this tab asks for the lists again.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refresh()
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    private fun render(state: FavoritesUiState) {
        val isGuest = state.load == FavoritesLoad.Guest
        binding.guestState.isVisible = isGuest
        if (isGuest) return

        val tabChanged = shownTab != null && shownTab != state.tab
        showTab(state.tab)
        if (tabChanged) fadeInTabContent()
        showHeader(state)
        showFilters(state)
        showCollections(state)

        val isDishes = state.tab == FavoritesTab.DISHES
        val total = if (isDishes) state.dishes.size else state.places.size
        val shown = if (isDishes) state.dishItems.size else state.placeItems.size
        when {
            state.load == FavoritesLoad.Loading -> showLoading()
            state.load is FavoritesLoad.Error -> showError(state.load.error)
            total == 0 -> showEmpty(isDishes)
            shown == 0 -> showNoResult()
            else -> binding.stateContainer.isVisible = false
        }
        val hasList = state.load == FavoritesLoad.Loaded && shown > 0
        binding.favoritesList.isVisible = hasList
        // A list that is not shown is emptied, so coming back to it does not flash old cards.
        val toRestore = pendingScrollState
        pendingScrollState = null
        val restore = Runnable {
            if (toRestore != null) binding.favoritesList.layoutManager?.onRestoreInstanceState(toRestore)
        }
        if (isDishes) {
            dishAdapter.submitList(if (hasList) state.dishItems else emptyList(), restore)
        } else {
            placeAdapter.submitList(if (hasList) state.placeItems else emptyList(), restore)
        }
    }

    private fun showTab(tab: FavoritesTab) {
        if (shownTab == tab) return
        shownTab?.let { scrollStates[it] = binding.favoritesList.layoutManager?.onSaveInstanceState() }
        with(binding.favoritesList) {
            adapter = null
            while (itemDecorationCount > 0) removeItemDecorationAt(0)
            val gap = resources.getDimensionPixelSize(R.dimen._12dp)
            when (tab) {
                FavoritesTab.DISHES -> {
                    layoutManager = GridLayoutManager(requireContext(), DISH_COLUMNS)
                    addItemDecoration(ListSpacingDecoration(DISH_COLUMNS, gap))
                    adapter = dishAdapter
                }
                FavoritesTab.PLACES -> {
                    layoutManager = LinearLayoutManager(requireContext())
                    addItemDecoration(ListSpacingDecoration(1, gap))
                    adapter = placeAdapter
                }
            }
        }
        pendingScrollState = scrollStates[tab]
        shownTab = tab
    }

    /**
     * Switching tabs rebuilds the collection cards and the list, so they fade in together instead of popping in
     * one by one as their photos arrive.
     */
    private fun fadeInTabContent() = with(binding) {
        listOf(collectionsSection, favoritesList, randomCard).forEach { view ->
            view.animate().cancel()
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(TAB_FADE_MS).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun showHeader(state: FavoritesUiState) = with(binding) {
        statDishes.text = state.dishes.size.toString()
        statPlaces.text = state.places.size.toString()
        statOpen.text = state.openCount.toString()
        tabDishes.text = getString(R.string.favorites_tab_dishes, state.dishes.size)
        tabPlaces.text = getString(R.string.favorites_tab_places, state.places.size)
        tabDishes.isSelected = state.tab == FavoritesTab.DISHES
        tabPlaces.isSelected = state.tab == FavoritesTab.PLACES
        if (searchInput.text?.toString() != state.query) {
            searchInput.setText(state.query)
            searchInput.setSelection(state.query.length)
        }
        searchClear.isVisible = state.query.isNotEmpty()
    }

    private fun showFilters(state: FavoritesUiState) = with(binding) {
        val isDishes = state.tab == FavoritesTab.DISHES
        dishFilterRow.isVisible = isDishes
        placeFilterRow.isVisible = !isDishes
        mealChips.forEach { (meal, chip) -> chip.isSelected = meal == state.mealFilter }
        chipPlaceAll.isSelected = state.placeFilter == PlaceFilter.ALL
        chipPlaceOpen.isSelected = state.placeFilter == PlaceFilter.OPEN_NOW
        sortLabel.setText(if (isDishes) state.dishSort.label else state.placeSort.label)
    }

    /** The random pick and the collections only make sense once something is saved, so they wait for the data. */
    private fun showCollections(state: FavoritesUiState) = with(binding) {
        val loaded = state.load == FavoritesLoad.Loaded
        randomCard.isVisible = loaded && state.tab == FavoritesTab.DISHES && state.dishes.isNotEmpty()
        val collections = if (loaded) state.collections else emptyList()
        collectionsSection.isVisible = collections.isNotEmpty()
        collectionAdapter.submitList(collections)
    }

    private fun showLoading() = with(binding) {
        stateContainer.isVisible = true
        favoritesLoading.isVisible = true
        stateBlock.isVisible = false
    }

    private fun showError(error: AppError) {
        val message = if (error.code == AppError.NETWORK) R.string.ai_chat_error_network else R.string.favorites_error_message
        showState(
            icon = R.drawable.ic_refresh,
            title = R.string.favorites_error_title,
            message = message,
            button = R.string.favorites_error_button,
        ) { viewModel.refresh() }
    }

    private fun showEmpty(isDishes: Boolean) {
        showState(
            icon = R.drawable.ic_favorite_border,
            title = if (isDishes) R.string.favorites_empty_dishes_title else R.string.favorites_empty_places_title,
            message = if (isDishes) R.string.favorites_empty_dishes_message else R.string.favorites_empty_places_message,
            button = R.string.favorites_empty_button,
        ) { (requireActivity() as? MainActivity)?.selectTab(MainTab.SEARCH) }
    }

    private fun showNoResult() {
        showState(
            icon = R.drawable.ic_search,
            title = R.string.favorites_no_result_title,
            message = R.string.favorites_no_result_message,
            button = R.string.favorites_no_result_button,
        ) { viewModel.clearFilters() }
    }

    private fun showState(
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes button: Int,
        action: () -> Unit,
    ) = with(binding) {
        stateContainer.isVisible = true
        favoritesLoading.isVisible = false
        stateBlock.isVisible = true
        stateIcon.setImageResource(icon)
        stateTitle.setText(title)
        stateMessage.setText(message)
        stateButton.setText(button)
        stateAction = action
    }

    private fun showSortMenu() {
        val state = viewModel.uiState.value
        val menu = PopupMenu(requireContext(), binding.sortButton)
        menu.menu.setGroupCheckable(0, true, true)
        if (state.tab == FavoritesTab.DISHES) {
            DishSort.entries.forEach { sort ->
                menu.menu.add(0, sort.ordinal, sort.ordinal, sort.label).isChecked = sort == state.dishSort
            }
            menu.setOnMenuItemClickListener { item ->
                viewModel.selectDishSort(DishSort.entries[item.itemId])
                true
            }
        } else {
            // Nearest first needs the device position, so it is offered only when one is known.
            PlaceSort.entries
                .filter { it != PlaceSort.NEAREST || state.position != null }
                .forEach { sort ->
                    menu.menu.add(0, sort.ordinal, sort.ordinal, sort.label).isChecked = sort == state.placeSort
                }
            menu.setOnMenuItemClickListener { item ->
                viewModel.selectPlaceSort(PlaceSort.entries[item.itemId])
                true
            }
        }
        menu.show()
    }

    private fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.Removed -> {
                removedSnackbar?.dismiss()
                removedSnackbar = Snackbar
                    .make(binding.favoritesCoordinator, getString(R.string.favorites_removed, event.name), Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.hue_secondary_container))
                    .setAction(R.string.favorites_undo) { viewModel.undoRemove() }
                    .also { it.show() }
            }
            FavoritesEvent.ActionFailed -> requireContext().makeToast(R.string.favorites_action_failed)
            is FavoritesEvent.Picked -> DishQuickViewSheet.show(childFragmentManager, event.dishId)
        }
    }

    private fun dial(phone: String) {
        try {
            requireContext().startDialer(phone)
        } catch (e: ActivityNotFoundException) {
            requireContext().makeToast(R.string.favorites_no_dialer)
        }
    }

    override fun onDestroyView() {
        removedSnackbar?.dismiss()
        removedSnackbar = null
        binding.favoritesList.adapter = null
        binding.collectionList.adapter = null
        shownTab = null
        pendingScrollState = null
        super.onDestroyView()
    }

    private companion object {
        const val DISH_COLUMNS = 2
        const val TAB_FADE_MS = 220L
    }
}
