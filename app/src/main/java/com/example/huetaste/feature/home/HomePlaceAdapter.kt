package com.example.huetaste.feature.home

import android.content.Context
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
import com.example.huetaste.common.extension.isOpenNow
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemHomePlaceCardBinding
import kotlin.math.roundToInt

/** The places of the home screen. Missing data only hides its own line. */
class HomePlaceAdapter(
    private val onPlaceClick: (HomePlaceItem) -> Unit,
) : LsListAdapter<HomePlaceItem, ItemHomePlaceCardBinding>(DiffCallback, ItemHomePlaceCardBinding::inflate) {

    override fun bindItem(item: HomePlaceItem, binding: ItemHomePlaceCardBinding, position: Int) = with(binding) {
        val context = root.context
        val place = item.place
        placeImage.contentDescription = place.name
        placeImage.loadWithShimmer(place.imageUrl)

        val isOpen = place.isOpenNow()
        placeOpenDot.isVisible = isOpen != null
        placeOpenText.isVisible = isOpen != null
        if (isOpen != null) {
            val color = ContextCompat.getColor(context, if (isOpen) R.color.hue_primary else R.color.place_closed)
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

        val distance = item.distanceKm?.let { distanceText(context, it, item.isDistanceApproximate) }
        val price = context.formatPriceRange(place.priceLevel, null)
        placeDistanceIcon.isVisible = distance != null
        placeDistance.isVisible = distance != null
        placeDistance.text = distance
        placeMetaSeparator.isVisible = distance != null && price != null
        placePrice.isVisible = price != null
        placePrice.text = price
        placeMetaRow.isVisible = distance != null || price != null

        root.clicks { onPlaceClick(item) }
    }

    /** An approximate position can be off by a few kilometers, so meters or decimals would promise too much. */
    private fun distanceText(context: Context, km: Double, isApproximate: Boolean): String =
        if (isApproximate) {
            val wholeKm = km.roundToInt().coerceAtLeast(1).toDouble()
            context.getString(R.string.home_place_distance_about, context.formatDistance(wholeKm))
        } else {
            context.formatDistance(km)
        }

    private object DiffCallback : DiffUtil.ItemCallback<HomePlaceItem>() {
        override fun areItemsTheSame(oldItem: HomePlaceItem, newItem: HomePlaceItem) = oldItem.place.id == newItem.place.id
        override fun areContentsTheSame(oldItem: HomePlaceItem, newItem: HomePlaceItem) = oldItem == newItem
    }
}
