package com.example.huetaste.common.extension

import android.widget.ImageView
import coil.load
import coil.request.Disposable
import coil.request.ImageRequest
import com.example.huetaste.R
import com.example.huetaste.common.ui.ImageShimmerDrawable

/** Loads a remote photo with the loading effect; a failed or missing photo shows the food placeholder. */
fun ImageView.loadWithShimmer(data: Any?, builder: ImageRequest.Builder.() -> Unit = {}): Disposable {
    val shimmer = ImageShimmerDrawable(context)
    return load(data) {
        placeholder(shimmer)
        error(R.drawable.ic_food_placeholder)
        fallback(R.drawable.ic_food_placeholder)
        builder()
    }
}
