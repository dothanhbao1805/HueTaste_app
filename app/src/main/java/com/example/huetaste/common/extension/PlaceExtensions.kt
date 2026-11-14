package com.example.huetaste.common.extension

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.domain.model.OpeningHours
import com.example.huetaste.domain.model.Place
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** `khoangCachKm` as meters below 1 km, otherwise as kilometers with one decimal. */
fun Context.formatDistance(km: Double): String = if (km < 1) {
    getString(R.string.ai_chat_distance_m, (km * 1000).roundToInt())
} else {
    val format = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply { maximumFractionDigits = 1 }
    getString(R.string.ai_chat_distance_km, format.format(km))
}

/** A rating with one decimal and a dot, such as `4.6`. */
fun formatRating(value: Double): String = String.format(Locale.US, "%.1f", value)

/** Whether the place is open now according to its `gioMoCua` lines, or null when they cannot be read. */
fun Place.isOpenNow(): Boolean? {
    val now = Calendar.getInstance()
    // Calendar counts Sunday as 1; OpeningHours counts Monday as 1 and Sunday as 7.
    val day = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    return OpeningHours.forDay(openingHours, day, minute)?.isOpenNow
}

/** Opens [place] in any installed map app, by coordinates when known and by name and address otherwise. */
fun Context.openMap(place: Place) {
    val lat = place.latitude
    val lng = place.longitude
    val query = Uri.encode(listOfNotNull(place.name, place.address).joinToString(", "))
    val uri = if (lat != null && lng != null) {
        Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(place.name)})")
    } else {
        Uri.parse("geo:0,0?q=$query")
    }
    try {
        startViewUri(uri)
    } catch (e: ActivityNotFoundException) {
        makeToast(R.string.dish_no_map_app)
    }
}
