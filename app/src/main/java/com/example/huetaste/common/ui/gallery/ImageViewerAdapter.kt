package com.example.huetaste.common.ui.gallery

import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemImageViewerPageBinding

/** Full-screen pages for [ImageViewerDialog]; every page owns an [ImageZoomController]. */
class ImageViewerAdapter(
    private val onSingleTap: () -> Unit,
) : LsListAdapter<String, ItemImageViewerPageBinding>(DiffCallback, ItemImageViewerPageBinding::inflate) {

    override fun bindItem(item: String, binding: ItemImageViewerPageBinding, position: Int) {
        val image = binding.viewerImage
        image.contentDescription = binding.root.context
            .getString(R.string.dish_image_page_description, position + 1, itemCount)
        val controller = image.getTag(R.id.tag_image_zoom_controller) as? ImageZoomController
            ?: ImageZoomController(image, onSingleTap).also { image.setTag(R.id.tag_image_zoom_controller, it) }
        // The hero carousel cached this url under the same key, so it appears instantly as the placeholder.
        controller.load(item, placeholderKey = item)
    }

    private object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
