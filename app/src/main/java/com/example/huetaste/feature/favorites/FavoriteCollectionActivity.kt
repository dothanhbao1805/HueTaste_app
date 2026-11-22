package com.example.huetaste.feature.favorites

import android.content.Context
import android.content.Intent
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.ui.ListSpacingDecoration
import com.example.huetaste.common.extension.openMap
import com.example.huetaste.common.extension.startDialer
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.databinding.ActivityFavoriteCollectionBinding
import com.example.huetaste.domain.model.AppError
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * One automatic collection of the Favorites tab ("Món sáng", "Ăn ngày mưa", "Đang mở cửa", "Gần bạn"…), opened
 * from its card with [newIntent]. It is its own Activity because it has its own back stack.
 */
@AndroidEntryPoint
class FavoriteCollectionActivity : LsActivity<ActivityFavoriteCollectionBinding>(ActivityFavoriteCollectionBinding::inflate) {
    private val viewModel: FavoriteCollectionViewModel by viewModels()

    private val dishAdapter = FavoriteDishAdapter(
        onDishClick = { item -> startDishDetailActivity(item.dish.id) },
        onRemoveClick = { item -> viewModel.removeDish(item.dish.id) },
    )
    private val placeAdapter = FavoritePlaceAdapter(
        onPlaceClick = { item -> startPlaceDetailActivity(item.place.id) },
        onRemoveClick = { item -> viewModel.removePlace(item.place.id) },
        onCallClick = { item -> item.place.phone?.let(::dial) },
        onDirectionsClick = { item -> openMap(item.place) },
    )

    private var removedSnackbar: Snackbar? = null

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isDishes = viewModel.uiState.value.collection.isDishes
        binding.collectionTitle.setText(viewModel.uiState.value.collection.title)
        with(binding.collectionList) {
            val gap = resources.getDimensionPixelSize(R.dimen._12dp)
            if (isDishes) {
                layoutManager = GridLayoutManager(this@FavoriteCollectionActivity, DISH_COLUMNS)
                addItemDecoration(ListSpacingDecoration(DISH_COLUMNS, gap))
                adapter = dishAdapter
            } else {
                layoutManager = LinearLayoutManager(this@FavoriteCollectionActivity)
                addItemDecoration(ListSpacingDecoration(1, gap))
                adapter = placeAdapter
            }
        }

        binding.backButton.clicks { handleBackPressed() }
        binding.sortButton.clicks { showSortMenu() }
        binding.stateButton.clicks { stateAction() }

        lifecycleScope.launch {
            // Hearts change on the detail screens, so every return reloads the lists.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refresh()
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    private fun render(state: FavoriteCollectionUiState) {
        val isDishes = state.collection.isDishes
        val count = state.itemCount
        binding.collectionSubtitle.text = getString(
            if (isDishes) R.string.favorites_col_saved_dishes else R.string.favorites_col_saved_places,
            count,
        )
        binding.sortLabel.setText(if (isDishes) state.dishSort.label else state.placeSort.label)

        val loading = state.load == FavoritesLoad.Loading
        val error = (state.load as? FavoritesLoad.Error)?.error
        binding.loadingProgress.isVisible = loading
        binding.stateBlock.isVisible = error != null || (state.load == FavoritesLoad.Loaded && count == 0)
        binding.collectionList.isVisible = state.load == FavoritesLoad.Loaded && count > 0
        binding.sortRow.isVisible = state.load == FavoritesLoad.Loaded && count > 0
        when {
            error != null -> {
                binding.stateTitle.setText(R.string.favorites_error_title)
                binding.stateMessage.setText(
                    if (error.code == AppError.NETWORK) R.string.ai_chat_error_network else R.string.favorites_error_message,
                )
                binding.stateButton.setText(R.string.favorites_error_button)
            }
            else -> {
                binding.stateTitle.setText(R.string.favorites_col_empty_title)
                binding.stateMessage.setText(R.string.favorites_col_empty_message)
                binding.stateButton.setText(R.string.favorites_col_back)
            }
        }
        if (isDishes) {
            dishAdapter.submitList(if (binding.collectionList.isVisible) state.dishItems else emptyList())
        } else {
            placeAdapter.submitList(if (binding.collectionList.isVisible) state.placeItems else emptyList())
        }
    }

    private fun stateAction() {
        if (viewModel.uiState.value.load is FavoritesLoad.Error) viewModel.refresh() else handleBackPressed()
    }

    private fun showSortMenu() {
        val state = viewModel.uiState.value
        val menu = PopupMenu(this, binding.sortButton)
        menu.menu.setGroupCheckable(0, true, true)
        if (state.collection.isDishes) {
            DishSort.entries.forEach { sort ->
                menu.menu.add(0, sort.ordinal, sort.ordinal, sort.label).isChecked = sort == state.dishSort
            }
            menu.setOnMenuItemClickListener { item ->
                viewModel.selectDishSort(DishSort.entries[item.itemId])
                true
            }
        } else {
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
                    .make(binding.collectionList, getString(R.string.favorites_removed, event.name), Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(this, R.color.hue_secondary_container))
                    .setAction(R.string.favorites_undo) { viewModel.undoRemove() }
                    .also { it.show() }
            }
            FavoritesEvent.ActionFailed -> makeToast(R.string.favorites_action_failed)
            is FavoritesEvent.Picked -> Unit
        }
    }

    private fun dial(phone: String) {
        try {
            startDialer(phone)
        } catch (e: android.content.ActivityNotFoundException) {
            makeToast(R.string.favorites_no_dialer)
        }
    }

    override fun onDestroy() {
        removedSnackbar?.dismiss()
        binding.collectionList.adapter = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_COLLECTION = "favorite_collection"
        private const val DISH_COLUMNS = 2

        fun newIntent(context: Context, collection: FavoriteCollection): Intent =
            Intent(context, FavoriteCollectionActivity::class.java).putExtra(EXTRA_COLLECTION, collection.name)
    }
}

internal val DishSort.label: Int
    @androidx.annotation.StringRes get() = when (this) {
        DishSort.RECENT -> R.string.favorites_sort_recent
        DishSort.PRICE_LOW -> R.string.favorites_sort_price_low
        DishSort.PRICE_HIGH -> R.string.favorites_sort_price_high
        DishSort.NAME -> R.string.favorites_sort_name
    }

internal val PlaceSort.label: Int
    @androidx.annotation.StringRes get() = when (this) {
        PlaceSort.RECENT -> R.string.favorites_sort_recent
        PlaceSort.NEAREST -> R.string.favorites_sort_nearest
        PlaceSort.RATING -> R.string.favorites_sort_rating
    }
