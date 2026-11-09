package com.example.huetaste.common.extension

import android.content.Context
import com.example.huetaste.R
import java.text.NumberFormat
import java.util.Locale

/** A price or a price range in VND, or null when neither end is known. */
fun Context.formatPriceRange(from: Int?, to: Int?): String? {
    val format = NumberFormat.getIntegerInstance(Locale("vi", "VN"))
    return when {
        from != null && to != null && from != to ->
            getString(R.string.dish_price_range, format.format(from), format.format(to))
        from != null -> getString(R.string.dish_price_value, format.format(from))
        to != null -> getString(R.string.dish_price_value, format.format(to))
        else -> null
    }
}
