package com.base.common.extension

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.FloatRange

fun Int.withAlpha(@FloatRange(from = 0.0, to = 1.0) alphaFraction: Float): Int {
    val newAlpha = (alphaFraction.coerceIn(0f, 1f) * 255).toInt()
    return Color.argb(newAlpha, Color.red(this), Color.green(this), Color.blue(this))
}