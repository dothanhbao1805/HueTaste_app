package com.example.huetaste.feature.home

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.formatPriceRange
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemHomeDishCardBinding

/** The suggested dishes. Missing text only hides its own line, so every card keeps the same height. */
class HomeDishAdapter(
    private val onDishClick: (HomeDishItem) -> Unit,
    private val onFavoriteClick: (HomeDishItem) -> Unit,
) : LsListAdapter<HomeDishItem, ItemHomeDishCardBinding>(DiffCallback, ItemHomeDishCardBinding::inflate) {

    override fun bindItem(item: HomeDishItem, binding: ItemHomeDishCardBinding, position: Int) = with(binding) {
        val context = root.context
        dishImage.contentDescription = item.dish.name
        dishImage.loadWithShimmer(item.dish.imageUrl)

        matchBadge.isVisible = item.matchPercent != null
        matchIcon.setImageResource(matchIconFor(position))
        matchText.text = item.matchPercent?.let { context.getString(R.string.home_match_format, it) }

        favoriteButton.isVisible = item.canFavorite
        favoriteButton.isSelected = item.isFavorite
        favoriteButton.clicks { onFavoriteClick(item) }

        categoryTag.isInvisible = item.categoryName == null
        categoryTag.text = item.categoryName
        dishName.text = item.dish.name
        dishReason.isInvisible = item.reason == null
        dishReason.text = item.reason

        val price = context.formatPriceRange(item.dish.priceFrom, item.dish.priceTo)
        dishPrice.isInvisible = price == null
        dishPrice.text = price

        root.clicks { onDishClick(item) }
    }

    private fun matchIconFor(position: Int) = when (position) {
        0 -> R.drawable.ic_fire
        1 -> R.drawable.ic_thumb_up
        else -> R.drawable.ic_auto_awesome
    }

    private object DiffCallback : DiffUtil.ItemCallback<HomeDishItem>() {
        override fun areItemsTheSame(oldItem: HomeDishItem, newItem: HomeDishItem) = oldItem.dish.id == newItem.dish.id
        override fun areContentsTheSame(oldItem: HomeDishItem, newItem: HomeDishItem) = oldItem == newItem
    }
}
