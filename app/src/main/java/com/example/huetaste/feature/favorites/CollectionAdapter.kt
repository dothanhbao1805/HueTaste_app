package com.example.huetaste.feature.favorites

import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemFavoriteCollectionBinding

/** The automatic collections of one tab, as cards in a sideways row. */
class CollectionAdapter(
    private val onClick: (CollectionSummary) -> Unit,
) : LsListAdapter<CollectionSummary, ItemFavoriteCollectionBinding>(DiffCallback, ItemFavoriteCollectionBinding::inflate) {

    override fun bindItem(item: CollectionSummary, binding: ItemFavoriteCollectionBinding, position: Int) = with(binding) {
        val context = root.context
        // No shimmer here: the card already has a solid surface, so a cover that is still loading fades in over it
        // instead of flashing a white placeholder when the tab changes and the cards are rebuilt.
        collectionCover.load(item.coverUrl) {
            crossfade(COVER_FADE_MS)
            placeholder(ColorDrawable(ContextCompat.getColor(context, R.color.hue_surface_container_high)))
            error(R.drawable.ic_food_placeholder)
            fallback(R.drawable.ic_food_placeholder)
        }
        collectionTitle.setText(item.collection.title)
        collectionCount.text = context.getString(
            if (item.collection.isDishes) R.string.favorites_col_count_dishes else R.string.favorites_col_count_places,
            item.count,
        )
        root.contentDescription = "${context.getString(item.collection.title)}, ${collectionCount.text}"
        root.clicks { onClick(item) }
    }

    private companion object {
        const val COVER_FADE_MS = 150
    }

    private object DiffCallback : DiffUtil.ItemCallback<CollectionSummary>() {
        override fun areItemsTheSame(oldItem: CollectionSummary, newItem: CollectionSummary) = oldItem.collection == newItem.collection
        override fun areContentsTheSame(oldItem: CollectionSummary, newItem: CollectionSummary) = oldItem == newItem
    }
}
