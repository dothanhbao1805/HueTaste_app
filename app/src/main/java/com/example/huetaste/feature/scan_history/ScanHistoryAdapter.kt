package com.example.huetaste.feature.scan_history

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import coil.load
import com.base.common.base.LsMultiListAdapter
import com.base.common.base.LsViewHolder
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemScanHistoryBinding
import com.example.huetaste.databinding.ItemScanHistoryHeaderBinding
import com.example.huetaste.databinding.ItemScanHistoryLoadingBinding
import com.example.huetaste.domain.model.ScanHistoryItem
import com.example.huetaste.domain.model.ScanStatus
import kotlin.math.roundToInt

/** One row of the scan history: a day header, a scan card or the next page loading. */
sealed interface ScanHistoryUiItem {
    val key: String

    data class Header(val label: String, val count: String) : ScanHistoryUiItem {
        override val key = "header_$label"
    }

    data class Card(val scan: ScanHistoryItem, val time: String) : ScanHistoryUiItem {
        override val key = "scan_${scan.id}"
    }

    data object LoadingMore : ScanHistoryUiItem {
        override val key = "loading_more"
    }
}

class ScanHistoryAdapter(
    private val onOpen: (ScanHistoryItem) -> Unit,
    private val onMore: (ScanHistoryItem) -> Unit,
) : LsMultiListAdapter<ScanHistoryUiItem, ViewBinding>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ScanHistoryUiItem.Header -> TYPE_HEADER
        is ScanHistoryUiItem.Card -> TYPE_CARD
        ScanHistoryUiItem.LoadingMore -> TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LsViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewBinding = when (viewType) {
            TYPE_HEADER -> ItemScanHistoryHeaderBinding.inflate(inflater, parent, false)
            TYPE_LOADING -> ItemScanHistoryLoadingBinding.inflate(inflater, parent, false)
            else -> ItemScanHistoryBinding.inflate(inflater, parent, false)
        }
        return LsViewHolder(binding)
    }

    override fun bindItem(item: ScanHistoryUiItem, binding: ViewBinding, position: Int) {
        when (item) {
            is ScanHistoryUiItem.Header -> with(binding as ItemScanHistoryHeaderBinding) {
                headerLabel.text = item.label
                headerCount.text = item.count
            }
            is ScanHistoryUiItem.Card -> bindCard(binding as ItemScanHistoryBinding, item)
            ScanHistoryUiItem.LoadingMore -> Unit
        }
    }

    private fun bindCard(binding: ItemScanHistoryBinding, item: ScanHistoryUiItem.Card) = with(binding) {
        val context = root.context
        val scan = item.scan
        scanImage.load(scan.imageUrl) {
            crossfade(IMAGE_FADE_MS)
            placeholder(ColorDrawable(ContextCompat.getColor(context, R.color.hue_surface_container_high)))
            error(R.drawable.ic_food_placeholder)
            fallback(R.drawable.ic_food_placeholder)
        }
        scanName.text = scan.name
        scanTime.text = item.time
        scanDescription.text = scan.description
        scanDescription.isVisible = scan.description != null

        val confidence = scan.confidence
        scanConfidence.isVisible = confidence != null
        if (confidence != null) {
            scanConfidence.text = context.getString(R.string.scan_history_match, (confidence * PERCENT).roundToInt())
        }

        val status = scan.status.chip
        scanStatusBox.backgroundTintList = ContextCompat.getColorStateList(context, status.background)
        scanStatus.setTextColor(ContextCompat.getColor(context, status.content))
        scanStatus.setText(status.label)
        scanStatusIcon.setImageResource(status.icon)
        scanStatusIcon.imageTintList = ContextCompat.getColorStateList(context, status.content)

        root.contentDescription = listOfNotNull(scan.name, item.time, context.getString(status.label)).joinToString(", ")
        root.clicks { onOpen(scan) }
        moreButton.clicks { onMore(scan) }
    }

    private class StatusChip(
        @param:StringRes val label: Int,
        @param:DrawableRes val icon: Int,
        @param:ColorRes val background: Int,
        @param:ColorRes val content: Int,
    )

    private val ScanStatus.chip
        get() = when (this) {
            ScanStatus.IN_CATALOG ->
                StatusChip(R.string.scan_history_in_catalog, R.drawable.ic_check_circle, R.color.hue_success_container, R.color.hue_success)
            ScanStatus.UPDATING ->
                StatusChip(R.string.scan_history_updating, R.drawable.ic_hourglass_empty, R.color.hue_secondary_fixed, R.color.hue_on_secondary_fixed_variant)
            ScanStatus.UNVERIFIED ->
                StatusChip(R.string.scan_history_unverified, R.drawable.ic_info, R.color.hue_surface_container_high, R.color.hue_on_surface_variant)
            ScanStatus.NOT_SAVED ->
                StatusChip(R.string.scan_history_not_saved, R.drawable.ic_info, R.color.hue_surface_container, R.color.hue_on_surface_variant)
        }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_CARD = 1
        const val TYPE_LOADING = 2
        const val IMAGE_FADE_MS = 150
        const val PERCENT = 100
    }

    private object DiffCallback : DiffUtil.ItemCallback<ScanHistoryUiItem>() {
        override fun areItemsTheSame(oldItem: ScanHistoryUiItem, newItem: ScanHistoryUiItem) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: ScanHistoryUiItem, newItem: ScanHistoryUiItem) = oldItem == newItem
    }
}
