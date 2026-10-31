package com.example.huetaste.feature.place_detail

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.huetaste.R
import kotlin.math.roundToInt

/** Colors the five [stars] so that the first [rating] (rounded) are filled and the rest are muted. */
internal fun bindStars(stars: List<ImageView>, rating: Double) {
    val filled = (rating.roundToInt()).coerceIn(0, stars.size)
    stars.forEachIndexed { index, star ->
        val color = if (index < filled) R.color.hue_secondary_container else R.color.hue_surface_container_highest
        star.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(star.context, color))
    }
}
