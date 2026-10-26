package com.base.common.extension

import android.content.Context
import android.util.TypedValue

fun Int.toDp(context: Context): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics)
}