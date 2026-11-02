package com.example.huetaste.common.extension

import android.view.View
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R

/** Binds a button whose feature is not built yet: it still gives the standard ripple and press animation. */
fun View.clicksComingSoon() {
    clicks { context.makeToast(R.string.coming_soon) }
}
