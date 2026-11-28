package com.example.huetaste.feature.context_suggestion

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.common.extension.formatPriceRange
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemSuggestedDishBinding

/** The dishes tab of the results screen. */
class SuggestedDishAdapter(
    private val onDishClick: (SuggestedDishItem) -> Unit,
    private val onFavoriteClick: (SuggestedDishItem) -> Unit,
) : LsListAdapter<SuggestedDishItem, ItemSuggestedDishBinding>(DiffCallback, ItemSuggestedDishBinding::inflate) {

    override fun bindItem(item: SuggestedDishItem, binding: ItemSuggestedDishBinding, position: Int) = with(binding) {
        val dish = item.dish
        rankBadge.showRank(item.rank)
        showMatch(matchBadge, matchText, item.matchPercent)

        dishImage.contentDescription = dish.name
        dishImage.loadWithShimmer(dish.imageUrl)
        dishName.text = dish.name
        dishDescription.isVisible = !dish.description.isNullOrBlank()
        dishDescription.text = dish.description

        val price = root.context.formatPriceRange(dish.priceFrom, dish.priceTo)
        dishPrice.isVisible = price != null
        dishPrice.text = price

        favoriteButton.isSelected = item.isFavorite
        favoriteButton.clicks { onFavoriteClick(item) }
        reasonBox.show(item.reason)
        root.clicks { onDishClick(item) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SuggestedDishItem>() {
        override fun areItemsTheSame(oldItem: SuggestedDishItem, newItem: SuggestedDishItem) = oldItem.dish.id == newItem.dish.id
        override fun areContentsTheSame(oldItem: SuggestedDishItem, newItem: SuggestedDishItem) = oldItem == newItem
    }
}
