package com.example.huetaste.feature.context_suggestion

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.formatDistance
import com.example.huetaste.common.extension.formatRating
import com.example.huetaste.common.extension.isOpenNow
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemSuggestedPlaceBinding

/** The places tab of the results screen. Missing data only hides its own part. */
class SuggestedPlaceAdapter(
    private val onPlaceClick: (SuggestedPlaceItem) -> Unit,
) : LsListAdapter<SuggestedPlaceItem, ItemSuggestedPlaceBinding>(DiffCallback, ItemSuggestedPlaceBinding::inflate) {

    override fun bindItem(item: SuggestedPlaceItem, binding: ItemSuggestedPlaceBinding, position: Int) = with(binding) {
        val context = root.context
        val place = item.place
        rankBadge.showRank(item.rank)
        showMatch(matchBadge, matchText, item.matchPercent)

        placeImage.contentDescription = place.name
        placeImage.loadWithShimmer(place.imageUrl)
        placeName.text = place.name
        placeAddress.isVisible = !place.address.isNullOrBlank()
        placeAddress.text = place.address

        val rating = place.rating
        placeRatingIcon.isVisible = rating != null
        placeRating.isVisible = rating != null
        placeRating.text = rating?.let(::formatRating)

        val distance = item.distanceKm?.let(context::formatDistance)
        placeDistanceIcon.isVisible = distance != null
        placeDistance.isVisible = distance != null
        placeDistance.text = distance

        val isOpen = place.isOpenNow()
        placeOpenText.isVisible = isOpen != null
        if (isOpen != null) {
            placeOpenText.setText(if (isOpen) R.string.home_open_now else R.string.ai_chat_closed)
            placeOpenText.setTextColor(ContextCompat.getColor(context, if (isOpen) R.color.hue_primary else R.color.place_closed))
        }
        placeMetaRow.isVisible = rating != null || distance != null || isOpen != null

        relatedRow.isVisible = item.relatedDishes.isNotEmpty()
        relatedText.text = context.getString(R.string.context_related_dishes, item.relatedDishes.joinToString(", "))

        reasonBox.show(item.reason)
        root.clicks { onPlaceClick(item) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SuggestedPlaceItem>() {
        override fun areItemsTheSame(oldItem: SuggestedPlaceItem, newItem: SuggestedPlaceItem) = oldItem.place.id == newItem.place.id
        override fun areContentsTheSame(oldItem: SuggestedPlaceItem, newItem: SuggestedPlaceItem) = oldItem == newItem
    }
}
