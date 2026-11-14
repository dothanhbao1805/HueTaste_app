package com.example.huetaste.feature.favorites

import androidx.annotation.StringRes
import com.example.huetaste.R

/** The backend `buoiPhuHop` codes; late night has no filter chip but still shows on a card. */
@StringRes
fun mealLabel(code: String): Int? = when (code) {
    "sang" -> R.string.favorites_meal_morning
    "trua" -> R.string.favorites_meal_noon
    "chieu" -> R.string.favorites_meal_afternoon
    "toi" -> R.string.favorites_meal_evening
    "khuya" -> R.string.favorites_meal_late
    else -> null
}

/** One weather label is enough: rain and cold first because they decide what people eat. */
@StringRes
fun weatherLabel(tags: List<String>): Int? = when {
    "mua" in tags -> R.string.favorites_weather_rain
    "lanh" in tags -> R.string.favorites_weather_cold
    "nong" in tags -> R.string.favorites_weather_hot
    "mat" in tags -> R.string.favorites_weather_cool
    else -> null
}
