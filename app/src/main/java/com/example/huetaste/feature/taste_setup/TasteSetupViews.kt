package com.example.huetaste.feature.taste_setup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemTasteChipBinding
import com.example.huetaste.databinding.ItemTasteOptionBinding
import com.example.huetaste.databinding.ViewTasteStatusBinding
import com.google.android.material.chip.ChipGroup

/** One selectable card of the taste setup pages. */
internal data class TasteOption(
    val key: String,
    val title: String,
    val subtitle: String?,
    @param:DrawableRes val icon: Int,
    val selected: Boolean,
)

/**
 * Shows [options] as cards in this container. Cards are rebuilt only when the set of options changes; otherwise
 * only their selection is updated, so a tap does not recreate the views under the finger.
 */
internal fun ViewGroup.bindOptions(options: List<TasteOption>, onClick: (TasteOption) -> Unit) {
    val keys = options.map { it.key }
    if (tag != keys) {
        removeAllViews()
        val inflater = LayoutInflater.from(context)
        options.forEach { option ->
            val item = ItemTasteOptionBinding.inflate(inflater, this, false)
            item.optionTitle.text = option.title
            item.optionSubtitle.text = option.subtitle
            item.optionSubtitle.isVisible = !option.subtitle.isNullOrBlank()
            item.optionIcon.setImageResource(option.icon)
            item.root.contentDescription = listOfNotNull(option.title, option.subtitle).joinToString(", ")
            item.root.clicks { onClick(option) }
            addView(item.root)
        }
        tag = keys
    }
    options.forEachIndexed { index, option ->
        val item = ItemTasteOptionBinding.bind(getChildAt(index))
        item.root.isSelected = option.selected
        item.optionCheck.isSelected = option.selected
    }
}

/** Shows [items] as selectable pills, rebuilding them only when the items change, like [bindOptions]. */
internal fun <T> ChipGroup.bindChips(
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

/** Shows loading, failure (tap to retry) or an empty catalog; hidden once a non-empty catalog is shown. */
internal fun ViewTasteStatusBinding.render(state: OptionsState, isEmpty: Boolean) {
    root.isVisible = state != OptionsState.READY || isEmpty
    root.isEnabled = state == OptionsState.FAILED
    statusSpinner.isVisible = state == OptionsState.LOADING
    statusText.setText(
        when (state) {
            OptionsState.LOADING -> R.string.taste_loading
            OptionsState.FAILED -> R.string.taste_load_failed
            OptionsState.READY -> R.string.taste_empty
        },
    )
}
