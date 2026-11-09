package com.example.huetaste.feature.home

import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.example.huetaste.R
import com.example.huetaste.common.extension.clicksComingSoon
import com.example.huetaste.databinding.ItemHomeCategoryBinding
import com.example.huetaste.domain.model.DishCategory
import java.util.Locale

/** The backend categories. The admin can rename them, so the icon follows words in the name and has a default. */
class HomeCategoryAdapter : LsListAdapter<DishCategory, ItemHomeCategoryBinding>(
    DiffCallback,
    ItemHomeCategoryBinding::inflate,
) {

    override fun bindItem(item: DishCategory, binding: ItemHomeCategoryBinding, position: Int) = with(binding) {
        val style = STYLES[position % STYLES.size]
        iconTile.setBackgroundResource(style.background)
        categoryIcon.setImageResource(iconFor(item.name))
        categoryIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(root.context, style.tint))
        categoryName.text = item.name
        // Search by category has no screen yet.
        root.clicksComingSoon()
    }

    @DrawableRes
    private fun iconFor(name: String): Int {
        val words = name.lowercase(Locale("vi", "VN"))
        fun has(vararg parts: String) = parts.any { it in words }
        return when {
            // Before "bánh": the noodle category is named "Bún - Bánh canh - Mì".
            has("bún", "mì", "phở", "bánh canh") -> R.drawable.ic_ramen_dining
            has("bánh") -> R.drawable.ic_bakery_dining
            has("cơm", "cháo") -> R.drawable.ic_rice_bowl
            has("chè", "tráng miệng") -> R.drawable.ic_icecream
            has("ăn vặt") -> R.drawable.ic_tapas
            has("chay") -> R.drawable.ic_eco
            else -> R.drawable.ic_restaurant
        }
    }

    private data class TileStyle(@DrawableRes val background: Int, @ColorRes val tint: Int)

    private object DiffCallback : DiffUtil.ItemCallback<DishCategory>() {
        override fun areItemsTheSame(oldItem: DishCategory, newItem: DishCategory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DishCategory, newItem: DishCategory) = oldItem == newItem
    }

    private companion object {
        val STYLES = listOf(
            TileStyle(R.drawable.bg_home_category_gold, R.color.hue_secondary),
            TileStyle(R.drawable.bg_home_category_primary, R.color.hue_primary),
            TileStyle(R.drawable.bg_home_category_neutral, R.color.hue_on_surface_variant),
        )
    }
}
