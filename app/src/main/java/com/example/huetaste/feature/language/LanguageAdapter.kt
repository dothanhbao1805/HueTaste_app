package com.example.huetaste.feature.language

import androidx.core.content.ContextCompat
import com.base.common.base.LsAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemLanguageBinding

/** Every [AppLanguage]; the row of [selected] is filled with the primary color. */
class LanguageAdapter(
    private val onLanguageClick: (AppLanguage) -> Unit,
) : LsAdapter<AppLanguage, ItemLanguageBinding>(ItemLanguageBinding::inflate) {

    var selected: AppLanguage = AppLanguage.VIETNAMESE
        set(value) {
            if (field == value) return
            val previous = data.indexOf(field)
            field = value
            if (previous >= 0) notifyItemChanged(previous)
            data.indexOf(value).takeIf { it >= 0 }?.let(::notifyItemChanged)
        }

    override fun bindItem(item: AppLanguage, binding: ItemLanguageBinding, position: Int) = with(binding) {
        val isSelected = item == selected
        root.isSelected = isSelected
        flag.setImageResource(item.flagRes)
        languageName.setText(item.titleRes)
        languageName.setTextColor(
            ContextCompat.getColor(root.context, if (isSelected) R.color.hue_on_primary else R.color.hue_on_surface),
        )
        root.clicks { onLanguageClick(item) }
    }
}
