package com.example.huetaste.common.extension

import android.content.Context
import com.example.huetaste.R

/** The backend weather codes of `NguCanh.thoiTiet`; anything else shows no weather. */
fun Context.weatherText(code: String?): String? = when (code) {
    "nong" -> getString(R.string.home_weather_hot)
    "mat" -> getString(R.string.home_weather_cool)
    "lanh" -> getString(R.string.home_weather_cold)
    "mua" -> getString(R.string.home_weather_rain)
    else -> null
}
