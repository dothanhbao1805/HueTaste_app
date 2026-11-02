package com.example.huetaste.common.extension

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.domain.model.Place

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
