package com.example.huetaste.common.extension

import android.view.View
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R

/** Binds a button whose feature is not built yet: it still gives the standard ripple and press animation. */
fun View.clicksComingSoon() {
    clicks { context.makeToast(R.string.coming_soon) }
}

private const val SHEET_HEIGHT_RATIO = 0.85f

/** Makes a bottom sheet's content take [SHEET_HEIGHT_RATIO] of the screen height instead of wrapping its content. */
fun View.applySheetHeight() {
    val height = (resources.displayMetrics.heightPixels * SHEET_HEIGHT_RATIO).toInt()
    layoutParams = (layoutParams ?: android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, height))
        .also { it.height = height }
}
