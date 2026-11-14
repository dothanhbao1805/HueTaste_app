package com.example.huetaste.feature.favorites

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsBottomSheet
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.applySheetHeight
import com.example.huetaste.common.extension.formatPriceRange
import com.example.huetaste.common.extension.formatRating
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.databinding.ItemQuickChipBinding
import com.example.huetaste.databinding.ItemQuickPlaceBinding
import com.example.huetaste.databinding.SheetDishQuickViewBinding
import com.example.huetaste.domain.model.DishDetail
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The Stitch "Yêu thích - Xem nhanh món" sheet: a saved dish at a glance, from `GET /mon-an/{id}`. The heart removes
 * the dish from the favorites through the Favorites screen's ViewModel, which outlives this sheet, so the removal and
 * its "Hoàn tác" offer do not depend on the sheet staying open.
 */
@AndroidEntryPoint
class DishQuickViewSheet : LsBottomSheet<SheetDishQuickViewBinding>(SheetDishQuickViewBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[DishQuickViewViewModel::class.java] }
    private val favorites by lazy { ViewModelProvider(requireParentFragment())[FavoritesViewModel::class.java] }

    override fun isFull(): Boolean = false

    override fun isAdjustResize(): Boolean = false

    override fun onViewCreated() {
        binding.root.applySheetHeight()
        binding.quickRetry.clicks { viewModel.load() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: DishQuickViewState) {
        binding.quickLoading.isVisible = state is DishQuickViewState.Loading
        binding.quickError.isVisible = state is DishQuickViewState.Error
        binding.quickContent.isVisible = state is DishQuickViewState.Content
        if (state is DishQuickViewState.Content) showDish(state.detail)
    }

    private fun showDish(detail: DishDetail) = with(binding) {
        val dish = detail.dish
        quickImage.contentDescription = dish.name
        quickImage.loadWithShimmer(detail.images.firstOrNull()?.url ?: dish.imageUrl)

        quickCategory.isVisible = !detail.categoryName.isNullOrBlank()
        quickCategory.text = detail.categoryName?.uppercase()
        quickName.text = dish.name
        val price = requireContext().formatPriceRange(dish.priceFrom, dish.priceTo)
        quickPrice.isVisible = price != null
        quickPrice.text = price

        quickFavorite.contentDescription = getString(R.string.favorites_remove_cd, dish.name)
        quickFavorite.clicks {
            favorites.removeDish(dish.id)
            dismiss()
        }

        val tags = dish.mealTimes.mapNotNull(::mealLabel).map { getString(it) } +
            listOfNotNull(weatherLabel(dish.weatherTags)?.let { getString(it) })
        fillChips(quickTags, tags)
        quickTags.isVisible = tags.isNotEmpty()

        fillChips(quickTastes, detail.tastes)
        quickTastesSection.isVisible = detail.tastes.isNotEmpty()

        val mainIngredients = detail.ingredients.filter { it.isMain }.map { it.name }
        fillChips(quickIngredients, mainIngredients)
        quickIngredientsSection.isVisible = mainIngredients.isNotEmpty()

        showPlaces(detail)
        quickDetailsButton.clicks {
            requireActivity().startDishDetailActivity(dish.id)
            dismiss()
        }
    }

    private fun showPlaces(detail: DishDetail) = with(binding) {
        quickPlaces.removeAllViews()
        quickPlacesSection.isVisible = detail.places.isNotEmpty()
        quickPlacesTitle.text = getString(R.string.favorites_quick_places, detail.places.size)
        detail.places.take(MAX_PLACES).forEach { dishPlace ->
            val row = ItemQuickPlaceBinding.inflate(layoutInflater, quickPlaces, false)
            val place = dishPlace.place
            row.quickPlaceImage.loadWithShimmer(place.imageUrl)
            row.quickPlaceName.text = place.name
            val price = requireContext().formatPriceRange(dishPlace.price, null)
            row.quickPlacePrice.isVisible = price != null
            row.quickPlacePrice.text = price
            row.quickPlaceStar.isVisible = place.rating != null
            row.quickPlaceRating.isVisible = place.rating != null
            row.quickPlaceRating.text = place.rating?.let(::formatRating)
            row.root.clicks { requireActivity().startPlaceDetailActivity(place.id) }
            quickPlaces.addView(row.root)
        }
    }

    private fun fillChips(group: ChipGroup, labels: List<String>) {
        group.removeAllViews()
        labels.forEach { label ->
            val chip = ItemQuickChipBinding.inflate(layoutInflater, group, false).root
            chip.text = label
            group.addView(chip)
        }
    }

    companion object {
        const val ARG_DISH_ID = "dish_id"
        private const val TAG = "dish_quick_view"
        private const val MAX_PLACES = 3

        fun show(fragmentManager: FragmentManager, dishId: String) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            DishQuickViewSheet().apply { arguments = bundleOf(ARG_DISH_ID to dishId) }.show(fragmentManager, TAG)
        }
    }
}
