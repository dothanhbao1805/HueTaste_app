package com.example.huetaste.feature.favorites

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.mealLabel
import com.example.huetaste.common.extension.weatherLabel
import com.example.huetaste.common.extension.formatPriceRange
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemFavoriteDishCardBinding

/** The saved dishes. Missing text only hides its own line, so cards in one row keep the same height. */
class FavoriteDishAdapter(
    private val onDishClick: (FavoriteDishItem) -> Unit,
    private val onRemoveClick: (FavoriteDishItem) -> Unit,
) : LsListAdapter<FavoriteDishItem, ItemFavoriteDishCardBinding>(DiffCallback, ItemFavoriteDishCardBinding::inflate) {

    override fun bindItem(item: FavoriteDishItem, binding: ItemFavoriteDishCardBinding, position: Int) = with(binding) {
        val context = root.context
        val dish = item.dish
        dishImage.contentDescription = dish.name
        dishImage.loadWithShimmer(dish.imageUrl)

        val meals = dish.mealTimes.mapNotNull { mealLabel(it) }.joinToString(MEAL_SEPARATOR) { context.getString(it) }
        dishMeal.isVisible = meals.isNotEmpty()
        dishMeal.text = meals

        favoriteButton.isSelected = true
        favoriteButton.contentDescription = context.getString(R.string.favorites_remove_cd, dish.name)
        favoriteButton.clicks { onRemoveClick(item) }

        categoryTag.isInvisible = item.categoryName == null
        categoryTag.text = item.categoryName
        dishName.text = dish.name
        dishDescription.text = dish.description?.takeIf { it.isNotBlank() }

        val weather = weatherLabel(dish.weatherTags)
        dishWeather.isInvisible = weather == null
        dishWeather.text = weather?.let { context.getString(it) }

        val price = context.formatPriceRange(dish.priceFrom, dish.priceTo)
        dishPrice.isInvisible = price == null
        dishPrice.text = price

        root.clicks { onDishClick(item) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FavoriteDishItem>() {
        override fun areItemsTheSame(oldItem: FavoriteDishItem, newItem: FavoriteDishItem) = oldItem.dish.id == newItem.dish.id
        override fun areContentsTheSame(oldItem: FavoriteDishItem, newItem: FavoriteDishItem) = oldItem == newItem
    }

    private companion object {
        const val MEAL_SEPARATOR = " • "
    }
}
