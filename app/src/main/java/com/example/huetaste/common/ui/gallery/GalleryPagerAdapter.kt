package com.example.huetaste.common.ui.gallery

import androidx.recyclerview.widget.DiffUtil
import coil.load
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemGalleryPageBinding
import com.example.huetaste.domain.model.GalleryImage

/** One page per photo in a hero carousel; tapping a page asks the screen to open the viewer. */
class GalleryPagerAdapter(
    private val onImageClick: (position: Int) -> Unit,
) : LsListAdapter<GalleryImage, ItemGalleryPageBinding>(DiffCallback, ItemGalleryPageBinding::inflate) {

    override fun bindItem(item: GalleryImage, binding: ItemGalleryPageBinding, position: Int) {
        binding.galleryImage.contentDescription = binding.root.context
            .getString(R.string.dish_image_page_description, position + 1, itemCount)
        binding.galleryImage.load(item.url) {
            crossfade(true)
            // The viewer reuses this bitmap as its placeholder while the full-size image loads.
            memoryCacheKey(item.url)
            placeholder(R.drawable.ic_food_placeholder)
            error(R.drawable.ic_food_placeholder)
        }
        binding.root.clicks { onImageClick(position) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GalleryImage>() {
        override fun areItemsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: GalleryImage, newItem: GalleryImage) = oldItem == newItem
    }
}
