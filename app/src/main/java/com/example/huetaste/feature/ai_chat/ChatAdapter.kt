package com.example.huetaste.feature.ai_chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.load
import com.base.common.base.LsMultiListAdapter
import com.base.common.base.LsViewHolder
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.Place
import com.example.huetaste.databinding.ItemChatAssistantBinding
import com.example.huetaste.databinding.ItemChatDateBinding
import com.example.huetaste.databinding.ItemChatDishRowBinding
import com.example.huetaste.databinding.ItemChatErrorBinding
import com.example.huetaste.databinding.ItemChatPlaceBinding
import com.example.huetaste.databinding.ItemChatTypingBinding
import com.example.huetaste.databinding.ItemChatUserBinding
import com.example.huetaste.databinding.ItemChatWelcomeBinding

/**
 * The assistant conversation: date headers, bubbles, dish rows, place cards and the typing and error states.
 *
 * @param onSuggestion called with the text of a quick question the user tapped.
 * @param onRetry called from the error bubble.
 */
class ChatAdapter(
    private val onSuggestion: (String) -> Unit,
    private val onRetry: () -> Unit,
    private val onDishClick: (Dish) -> Unit,
    private val onPlaceClick: (Place) -> Unit,
    private val onDirections: (Place) -> Unit,
) : LsMultiListAdapter<ChatUiItem, ViewBinding>(DiffCallback) {

    /** Dish rows share one pool so their cards are recycled across replies. */
    private val dishCardPool = RecyclerView.RecycledViewPool()

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ChatUiItem.DateHeader -> TYPE_DATE
        ChatUiItem.Welcome -> TYPE_WELCOME
        is ChatUiItem.UserMessage -> TYPE_USER
        is ChatUiItem.AssistantMessage -> TYPE_ASSISTANT
        is ChatUiItem.DishCards -> TYPE_DISHES
        is ChatUiItem.PlaceCard -> TYPE_PLACE
        ChatUiItem.Typing -> TYPE_TYPING
        is ChatUiItem.Error -> TYPE_ERROR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LsViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewBinding = when (viewType) {
            TYPE_DATE -> ItemChatDateBinding.inflate(inflater, parent, false)
            TYPE_WELCOME -> ItemChatWelcomeBinding.inflate(inflater, parent, false).also(::bindSuggestions)
            TYPE_USER -> ItemChatUserBinding.inflate(inflater, parent, false)
            TYPE_ASSISTANT -> ItemChatAssistantBinding.inflate(inflater, parent, false)
            TYPE_DISHES -> ItemChatDishRowBinding.inflate(inflater, parent, false).apply {
                dishList.setRecycledViewPool(dishCardPool)
                dishList.adapter = ChatDishCardAdapter(onDishClick)
            }
            TYPE_PLACE -> ItemChatPlaceBinding.inflate(inflater, parent, false)
            TYPE_TYPING -> ItemChatTypingBinding.inflate(inflater, parent, false)
            else -> ItemChatErrorBinding.inflate(inflater, parent, false)
        }
        return LsViewHolder(binding)
    }

    override fun bindItem(item: ChatUiItem, binding: ViewBinding, position: Int) {
        when (item) {
            is ChatUiItem.DateHeader ->
                (binding as ItemChatDateBinding).dateText.text = ChatFormat.dayLabel(binding.root.context, item.dayStart)
            ChatUiItem.Welcome, ChatUiItem.Typing -> Unit
            is ChatUiItem.UserMessage -> with(binding as ItemChatUserBinding) {
                messageText.text = item.text
                // A question still on its way is dimmed and shows no time yet.
                messageText.alpha = if (item.pending) PENDING_ALPHA else 1f
                timeText.isVisible = !item.pending
                timeText.text = ChatFormat.time(item.time)
            }
            is ChatUiItem.AssistantMessage -> with(binding as ItemChatAssistantBinding) {
                messageText.text = item.text
                timeText.text = ChatFormat.time(item.time)
            }
            is ChatUiItem.DishCards ->
                ((binding as ItemChatDishRowBinding).dishList.adapter as ChatDishCardAdapter).submitList(item.cards)
            is ChatUiItem.PlaceCard -> bindPlace(item, binding as ItemChatPlaceBinding)
            is ChatUiItem.Error -> with(binding as ItemChatErrorBinding) {
                errorText.text = errorMessage(item, root.context)
                retryButton.isVisible = item.canRetry
                retryButton.clicks { onRetry() }
            }
        }
    }

    private fun errorMessage(item: ChatUiItem.Error, context: android.content.Context): String = when (item.code) {
        AppError.NETWORK -> context.getString(R.string.ai_chat_error_network)
        AppError.UNAUTHORIZED, AppError.INVALID_TOKEN, AppError.TOKEN_EXPIRED ->
            context.getString(R.string.ai_chat_sign_in_message)
        AppError.UNKNOWN -> context.getString(R.string.ai_chat_error)
        else -> item.message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.ai_chat_error)
    }

    private fun bindSuggestions(binding: ItemChatWelcomeBinding) = with(binding) {
        listOf(
            suggestionLunch to suggestionLunchText,
            suggestionNearby to suggestionNearbyText,
            suggestionMild to suggestionMildText,
            suggestionBudget to suggestionBudgetText,
        ).forEach { (chip, label) -> chip.clicks { onSuggestion(label.text.toString()) } }
    }

    private fun bindPlace(item: ChatUiItem.PlaceCard, binding: ItemChatPlaceBinding) = with(binding) {
        val context = root.context
        val place = item.place
        placeImage.load(place.imageUrl) {
            placeholder(R.drawable.ic_food_placeholder)
            error(R.drawable.ic_food_placeholder)
            fallback(R.drawable.ic_food_placeholder)
        }
        placeName.text = place.name
        placeReason.isVisible = !item.reason.isNullOrBlank()
        placeReason.text = item.reason

        val isOpen = ChatFormat.isOpenNow(place.openingHours)
        openBadge.isVisible = isOpen != null
        if (isOpen != null) {
            openText.setText(if (isOpen) R.string.ai_chat_open else R.string.ai_chat_closed)
            openDot.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, if (isOpen) R.color.place_open else R.color.place_closed),
            )
        }
        ratingBadge.isVisible = place.rating != null
        ratingText.text = place.rating?.let(ChatFormat::rating)

        placeAddress.isVisible = !place.address.isNullOrBlank()
        placeAddress.text = place.address
        placeDistance.isVisible = item.distanceKm != null
        placeDistance.text = item.distanceKm?.let { ChatFormat.distance(context, it) }
        val price = ChatFormat.price(context, place.priceLevel, null)
        priceRow.isVisible = price != null
        placePrice.text = price

        placeCard.clicks { onPlaceClick(place) }
        directionsButton.clicks { onDirections(place) }
        viewPlaceButton.clicks { onPlaceClick(place) }
    }

    override fun onViewAttachedToWindow(holder: LsViewHolder<ViewBinding>) {
        super.onViewAttachedToWindow(holder)
        val binding = holder.binding as? ItemChatTypingBinding ?: return
        (binding.root.getTag(R.id.chat_typing_animator) as? Animator)?.cancel()
        val animator = typingAnimator(binding.dot1, binding.dot2, binding.dot3)
        binding.root.setTag(R.id.chat_typing_animator, animator)
        animator.start()
    }

    override fun onViewDetachedFromWindow(holder: LsViewHolder<ViewBinding>) {
        val binding = holder.binding as? ItemChatTypingBinding
        if (binding != null) {
            (binding.root.getTag(R.id.chat_typing_animator) as? Animator)?.cancel()
            binding.root.setTag(R.id.chat_typing_animator, null)
        }
        super.onViewDetachedFromWindow(holder)
    }

    /** The three dots fade in turn while the assistant is answering. */
    private fun typingAnimator(vararg dots: View): Animator = AnimatorSet().apply {
        playTogether(
            dots.mapIndexed { index, dot ->
                ObjectAnimator.ofFloat(dot, View.ALPHA, DOT_MIN_ALPHA, 1f).apply {
                    duration = DOT_FADE_MS
                    startDelay = index * DOT_STAGGER_MS
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                }
            },
        )
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatUiItem>() {
        override fun areItemsTheSame(oldItem: ChatUiItem, newItem: ChatUiItem) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: ChatUiItem, newItem: ChatUiItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_DATE = 0
        const val TYPE_WELCOME = 1
        const val TYPE_USER = 2
        const val TYPE_ASSISTANT = 3
        const val TYPE_DISHES = 4
        const val TYPE_PLACE = 5
        const val TYPE_TYPING = 6
        const val TYPE_ERROR = 7

        const val DOT_MIN_ALPHA = 0.3f
        const val PENDING_ALPHA = 0.6f
        const val DOT_FADE_MS = 450L
        const val DOT_STAGGER_MS = 150L
    }
}
