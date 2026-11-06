package com.example.huetaste.feature.ai_chat

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.base.common.base.LsListAdapter
import com.example.huetaste.R
import com.base.common.extension.clicks
import com.example.huetaste.databinding.ItemChatDishCardBinding
import com.example.huetaste.domain.model.Dish

/** The sideways row of dish cards under one assistant reply. */
class ChatDishCardAdapter(
    private val onDishClick: (Dish) -> Unit,
) : LsListAdapter<ChatDishCard, ItemChatDishCardBinding>(
    DiffCallback,
    ItemChatDishCardBinding::inflate,
) {

    override fun bindItem(item: ChatDishCard, binding: ItemChatDishCardBinding, position: Int) {
        val context = binding.root.context
        val dish = item.dish
        binding.dishImage.load(dish.imageUrl) {
            placeholder(R.drawable.ic_food_placeholder)
            error(R.drawable.ic_food_placeholder)
            fallback(R.drawable.ic_food_placeholder)
        }
        binding.matchBadge.isVisible = item.matchPercent != null
        binding.matchBadge.text = item.matchPercent?.let { context.getString(R.string.ai_chat_match, it) }
        binding.dishName.text = dish.name
        val price = ChatFormat.price(context, dish.priceFrom, dish.priceTo)
        binding.dishPrice.isVisible = price != null
        binding.dishPrice.text = price
        binding.dishReason.isVisible = !item.reason.isNullOrBlank()
        binding.dishReason.text = item.reason
        binding.root.clicks { onDishClick(dish) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatDishCard>() {
        override fun areItemsTheSame(oldItem: ChatDishCard, newItem: ChatDishCard) = oldItem.dish.id == newItem.dish.id
        override fun areContentsTheSame(oldItem: ChatDishCard, newItem: ChatDishCard) = oldItem == newItem
    }
}
