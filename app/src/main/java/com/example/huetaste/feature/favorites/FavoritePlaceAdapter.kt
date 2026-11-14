package com.example.huetaste.feature.favorites

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.formatDistance
import com.example.huetaste.common.extension.formatPriceRange
import com.example.huetaste.common.extension.formatRating
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemFavoritePlaceCardBinding

/** The saved places. Missing data only hides its own line; the call button needs a phone number. */
class FavoritePlaceAdapter(
    private val onPlaceClick: (FavoritePlaceItem) -> Unit,
    private val onRemoveClick: (FavoritePlaceItem) -> Unit,
    private val onCallClick: (FavoritePlaceItem) -> Unit,
    private val onDirectionsClick: (FavoritePlaceItem) -> Unit,
) : LsListAdapter<FavoritePlaceItem, ItemFavoritePlaceCardBinding>(DiffCallback, ItemFavoritePlaceCardBinding::inflate) {

    override fun bindItem(item: FavoritePlaceItem, binding: ItemFavoritePlaceCardBinding, position: Int) = with(binding) {
        val context = root.context
        val place = item.place
        placeImage.contentDescription = place.name
        placeImage.loadWithShimmer(place.imageUrl)

        val isOpen = item.isOpen
        placeOpenDot.isVisible = isOpen != null
        placeOpenText.isVisible = isOpen != null
        if (isOpen != null) {
            val color = ContextCompat.getColor(context, if (isOpen) R.color.place_open else R.color.place_closed)
            placeOpenDot.backgroundTintList = ColorStateList.valueOf(color)
            placeOpenText.setTextColor(color)
            placeOpenText.setText(if (isOpen) R.string.home_open_now else R.string.ai_chat_closed)
        }

        placeRatingIcon.isVisible = place.rating != null
        placeRating.isVisible = place.rating != null
        placeRating.text = place.rating?.let(::formatRating)

        placeName.text = place.name
        placeAddress.isVisible = !place.address.isNullOrBlank()
        placeAddress.text = place.address

        val distance = item.distanceKm?.let(context::formatDistance)
        val price = context.formatPriceRange(place.priceLevel, null)
        placeDistanceIcon.isVisible = distance != null
        placeDistance.isVisible = distance != null
        placeDistance.text = distance
        placeMetaSeparator.isVisible = distance != null && price != null
        placePrice.isVisible = price != null
        placePrice.text = price
        placeMetaRow.isVisible = distance != null || price != null

        favoriteButton.contentDescription = context.getString(R.string.favorites_remove_cd, place.name)
        favoriteButton.clicks { onRemoveClick(item) }

        callButton.isVisible = place.phone != null
        callButton.clicks { onCallClick(item) }
        directionsButton.clicks { onDirectionsClick(item) }

        root.clicks { onPlaceClick(item) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FavoritePlaceItem>() {
        override fun areItemsTheSame(oldItem: FavoritePlaceItem, newItem: FavoritePlaceItem) = oldItem.place.id == newItem.place.id
        override fun areContentsTheSame(oldItem: FavoritePlaceItem, newItem: FavoritePlaceItem) = oldItem == newItem
    }
}
