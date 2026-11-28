package com.example.huetaste.feature.context_suggestion

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.huetaste.R
import com.example.huetaste.databinding.ViewSuggestionReasonBinding

/** The "#1" pill: the first card stands out in the primary color. */
internal fun TextView.showRank(rank: Int) {
    val first = rank == 1
    text = context.getString(R.string.context_rank_format, rank)
    backgroundTintList = ContextCompat.getColorStateList(
        context,
        if (first) R.color.hue_primary else R.color.hue_surface_container_high,
    )
    setTextColor(ContextCompat.getColor(context, if (first) R.color.hue_on_primary else R.color.hue_on_surface))
}

internal fun showMatch(badge: View, text: TextView, percent: Int?) {
    badge.isVisible = percent != null
    text.text = percent?.let { text.context.getString(R.string.home_match_format, it) }
}

internal fun ViewSuggestionReasonBinding.show(reason: String?) {
    root.isVisible = !reason.isNullOrBlank()
    reasonText.text = reason
}
