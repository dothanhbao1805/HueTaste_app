package com.example.huetaste.common.ui.gallery

import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.databinding.ItemGalleryPageBinding
import com.example.huetaste.domain.model.GalleryImage

/**
 * One page per photo in a hero carousel; tapping a page asks the screen to open the viewer. While a page is held,
 * [pressTarget] (the card around the carousel) shrinks instead of the photo alone, so the card's rounded frame
 * and the photo move together and no rectangular photo edge shows inside the card.
 */
class GalleryPagerAdapter(
    private val pressTarget: () -> View?,
    private val onImageClick: (position: Int) -> Unit,
) : LsListAdapter<GalleryImage, ItemGalleryPageBinding>(DiffCallback, ItemGalleryPageBinding::inflate) {

    override fun bindItem(item: GalleryImage, binding: ItemGalleryPageBinding, position: Int) {
        binding.galleryImage.contentDescription = binding.root.context
            .getString(R.string.dish_image_page_description, position + 1, itemCount)
        binding.galleryImage.loadWithShimmer(item.url) {
            crossfade(true)
            // The viewer reuses this bitmap as its placeholder while the full-size image loads.
            memoryCacheKey(item.url)
            // Links to other sites can die after they were collected; the smaller copy usually still loads.
            listener(onError = { _, _ ->
                item.thumbnailUrl?.let { thumbnail -> binding.galleryImage.loadWithShimmer(thumbnail) }
            })
        }
        binding.root.clicks(withAnim = false) { onImageClick(position) }
        binding.root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> animatePress(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePress(false)
            }
            false
        }
    }

    private fun animatePress(pressed: Boolean) {
        pressTarget()?.animate()
            ?.scaleX(if (pressed) PRESSED_SCALE else 1f)
            ?.scaleY(if (pressed) PRESSED_SCALE else 1f)
            ?.setDuration(if (pressed) PRESS_MS else RELEASE_MS)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()
    }

    private companion object {
        const val PRESSED_SCALE = 0.97f
        const val PRESS_MS = 100L
        const val RELEASE_MS = 150L
    }

    private object DiffCallback : DiffUtil.ItemCallback<GalleryImage>() {
        override fun areItemsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem == newItem
    }
}
