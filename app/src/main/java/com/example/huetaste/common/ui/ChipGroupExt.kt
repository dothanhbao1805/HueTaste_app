package com.example.huetaste.common.ui

import android.view.LayoutInflater
import com.base.common.extension.clicks
import com.example.huetaste.databinding.ItemTasteChipBinding
import com.google.android.material.chip.ChipGroup

/** Shows [items] as selectable pills, rebuilding them only when the items change. */
fun <T> ChipGroup.bindChips(
    items: List<T>,
    key: (T) -> String,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onClick: (T) -> Unit,
) {
    val keys = items.map(key)
    if (tag != keys) {
        removeAllViews()
        val inflater = LayoutInflater.from(context)
        items.forEach { item ->
            val chip = ItemTasteChipBinding.inflate(inflater, this, false).root
            chip.text = label(item)
            chip.clicks { onClick(item) }
            addView(chip)
        }
        tag = keys
    }
    items.forEachIndexed { index, item -> getChildAt(index).isSelected = isSelected(item) }
}
