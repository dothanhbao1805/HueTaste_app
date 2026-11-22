package com.example.huetaste.feature.search

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.base.common.base.LsMultiListAdapter
import com.base.common.base.LsViewHolder
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.formatDistance
import com.example.huetaste.common.extension.formatPriceRange
import com.example.huetaste.common.extension.formatRating
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.common.extension.mealLabel
import com.example.huetaste.common.extension.todayHours
import com.example.huetaste.common.extension.weatherLabel
import com.example.huetaste.databinding.ItemSearchDishBinding
import com.example.huetaste.databinding.ItemSearchLoadingBinding
import com.example.huetaste.databinding.ItemSearchPlaceBinding
import com.example.huetaste.databinding.ItemSearchTagBinding
import com.example.huetaste.domain.model.DishSearchItem
import com.example.huetaste.domain.model.PlaceArea
import com.example.huetaste.domain.model.PlaceSearchItem

/** One row of the search results: a dish, a place, or the next page loading. */
sealed interface SearchResultItem {
    val key: String

    /** [canFavorite] is false for a guest, whose hearts are hidden as on the home screen. */
    data class Dish(val item: DishSearchItem, val isFavorite: Boolean, val canFavorite: Boolean) : SearchResultItem {
        override val key = "dish_${item.dish.id}"
    }

    data class Place(val item: PlaceSearchItem, val isFavorite: Boolean, val canFavorite: Boolean) : SearchResultItem {
        override val key = "place_${item.place.id}"
    }

    data object LoadingMore : SearchResultItem {
        override val key = "loading_more"
    }
}

class SearchResultAdapter(
    private val onDishClick: (DishSearchItem) -> Unit,
    private val onDishFavorite: (DishSearchItem) -> Unit,
    private val onPlaceClick: (PlaceSearchItem) -> Unit,
    private val onPlaceFavorite: (PlaceSearchItem) -> Unit,
    private val onDirections: (PlaceSearchItem) -> Unit,
) : LsMultiListAdapter<SearchResultItem, ViewBinding>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SearchResultItem.Dish -> TYPE_DISH
        is SearchResultItem.Place -> TYPE_PLACE
        SearchResultItem.LoadingMore -> TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LsViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewBinding = when (viewType) {
            TYPE_DISH -> ItemSearchDishBinding.inflate(inflater, parent, false)
            TYPE_PLACE -> ItemSearchPlaceBinding.inflate(inflater, parent, false)
            else -> ItemSearchLoadingBinding.inflate(inflater, parent, false)
        }
        return LsViewHolder(binding)
    }

    override fun bindItem(item: SearchResultItem, binding: ViewBinding, position: Int) {
        when (item) {
            is SearchResultItem.Dish -> bindDish(binding as ItemSearchDishBinding, item)
            is SearchResultItem.Place -> bindPlace(binding as ItemSearchPlaceBinding, item)
            SearchResultItem.LoadingMore -> Unit
        }
    }

    private fun bindDish(binding: ItemSearchDishBinding, row: SearchResultItem.Dish) = with(binding) {
        val context = root.context
        val dish = row.item.dish
        dishImage.contentDescription = dish.name
        dishImage.loadWithShimmer(dish.imageUrl)
        dishSignature.isVisible = row.item.isSignature
        dishName.text = dish.name
        dishDescription.isVisible = !dish.description.isNullOrBlank()
        dishDescription.text = dish.description

        val tags = dish.mealTimes.mapNotNull(::mealLabel) + listOfNotNull(weatherLabel(dish.weatherTags))
        dishTags.isVisible = tags.isNotEmpty()
        dishTags.removeAllViews()
        val inflater = LayoutInflater.from(context)
        tags.forEach { label ->
            dishTags.addView(ItemSearchTagBinding.inflate(inflater, dishTags, false).root.apply { setText(label) })
        }

        val price = context.formatPriceRange(dish.priceFrom, dish.priceTo)
        // The weight keeps the place count at the end even without a price.
        dishPrice.text = price.orEmpty()
        val count = row.item.placeCount
        dishPlaceIcon.isVisible = count > 0
        dishPlaceCount.isVisible = count > 0
        dishPlaceCount.text = context.getString(R.string.search_dish_place_count, count)

        bindHeart(favoriteButton, row.isFavorite, row.canFavorite, dish.name) { onDishFavorite(row.item) }
        root.clicks { onDishClick(row.item) }
    }

    private fun bindPlace(binding: ItemSearchPlaceBinding, row: SearchResultItem.Place) = with(binding) {
        val context = root.context
        val place = row.item.place
        placeImage.contentDescription = place.name
        placeImage.loadWithShimmer(place.imageUrl)

        placeRatingChip.isVisible = place.rating != null
        placeRating.text = place.rating?.let(::formatRating)
        val hours = place.todayHours()
        placeHours.isVisible = hours != null
        placeHours.text = hours?.let { context.getString(R.string.search_place_hours_today, it.text) }
        val level = place.priceLevel?.takeIf { it > 0 }
        placePrice.isVisible = level != null
        placePrice.text = level?.let { PRICE_SYMBOL.repeat(it.coerceAtMost(MAX_PRICE_LEVEL)) }

        placeName.text = place.name
        val isOpen = hours?.isOpenNow
        placeOpenChip.isVisible = isOpen != null
        if (isOpen != null) {
            val color = ContextCompat.getColor(context, if (isOpen) R.color.place_open else R.color.place_closed)
            placeOpenChip.backgroundTintList = ColorStateList.valueOf(color).withAlpha(CHIP_ALPHA)
            placeOpenDot.backgroundTintList = ColorStateList.valueOf(color)
            placeOpenText.setTextColor(color)
            placeOpenText.setText(if (isOpen) R.string.home_open_now else R.string.ai_chat_closed)
        }
        placeAddressRow.isVisible = !place.address.isNullOrBlank()
        placeAddress.text = place.address

        val distance = row.item.distanceKm?.let(context::formatDistance)
        val area = row.item.area?.let { context.getString(it.shortLabel) }
        placeDistanceIcon.isVisible = distance != null || area != null
        placeDistance.text = when {
            distance != null && area != null -> context.getString(R.string.search_place_distance_area, distance, area)
            distance != null -> context.getString(R.string.search_place_distance, distance)
            else -> area
        }

        bindHeart(favoriteButton, row.isFavorite, row.canFavorite, place.name) { onPlaceFavorite(row.item) }
        directionsButton.clicks { onDirections(row.item) }
        root.clicks { onPlaceClick(row.item) }
    }

    private fun bindHeart(
        button: android.widget.ImageView,
        isFavorite: Boolean,
        canFavorite: Boolean,
        name: String,
        onClick: () -> Unit,
    ) {
        button.isVisible = canFavorite
        button.isSelected = isFavorite
        button.contentDescription = button.context.getString(
            if (isFavorite) R.string.favorites_remove_cd else R.string.search_favorite_add_cd,
            name,
        )
        button.clicks { onClick() }
    }

    private val PlaceArea.shortLabel
        get() = when (this) {
            PlaceArea.CO_DO -> R.string.search_area_short_co_do
            PlaceArea.BO_BAC -> R.string.search_area_short_bo_bac
            PlaceArea.BO_NAM -> R.string.search_area_short_bo_nam
        }

    private companion object {
        const val TYPE_DISH = 0
        const val TYPE_PLACE = 1
        const val TYPE_LOADING = 2
        const val PRICE_SYMBOL = "₫"
        const val MAX_PRICE_LEVEL = 4
        const val CHIP_ALPHA = 31 // about 12% of the status color behind its text
    }

    private object DiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
        override fun areItemsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem) = oldItem == newItem
    }
}
