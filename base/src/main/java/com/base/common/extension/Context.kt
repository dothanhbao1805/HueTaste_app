package com.base.common.extension

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.BoolRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.base.R
import com.base.common.base.LsApplication
import timber.log.Timber
import java.util.Locale

fun Context.getStringByLocale(@StringRes resId: Int, vararg formatArgs: Any): String {
    val config = Configuration(this.resources.configuration)
    config.setLocale(Locale(LsApplication.app.prefs.language.get()))
    val localizedContext = this.createConfigurationContext(config)
    return localizedContext.resources.getString(resId, *formatArgs)
}

@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun Context.statusHeight(dimen: Int = com.intuit.sdp.R.dimen._30sdp): Int {
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else {
        Timber.e("statusHeight", "dimen")
        dimenToInt(dimen)
    }
}

fun Context.dimenToInt(dimen: Int): Int {
    return resources.getDimension(dimen).toInt()
}

fun String?.toUriAssetsIfAvailable(): String {
    return when {
        this == null -> ""
        this.startsWith("https:") || this.endsWith(".png") -> this
        else -> "file:///android_asset/$this"
    }
}

fun String?.toStringLocalIfAvailable(context: Context): String {
    return when {
        this == null -> ""
        this.endsWith(".xml") -> context.getStringByName(this.replace(".xml", ""))
        else -> this
    }
}

fun Context.getStringByName(name: String): String {
    val prefs = LsApplication.app.prefs

    val localizedContext = if (prefs.language.get().isNotEmpty()) {
        val locale = Locale(prefs.language.get())
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)
    } else {
        this
    }

    val resId = localizedContext.resources.getIdentifier(name, "string", packageName)
    return localizedContext.getString(resId)
}

fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}

fun Context.isNightMode() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

inline fun <T> tryOrNull(logOnError: Boolean = true, body: () -> T?): T? {
    return try {
        body()
    } catch (e: Exception) {
        if (logOnError) {
            Timber.e("Error: $e")
        }
        null
    }
}

@ColorInt
fun Context.resolveAttrColor(@AttrRes attr: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attr))
    val color: Int
    try {
        color = a.getColor(0, 0)
    } finally {
        a.recycle()
    }
    return color
}

fun Context.resolveAttrBoolean(@AttrRes attr: Int): Boolean {
    val a = theme.obtainStyledAttributes(intArrayOf(attr))
    val boolean: Boolean
    try {
        boolean = a.getBoolean(0, false)
    } finally {
        a.recycle()
    }
    return boolean
}

fun Context.getDimens(@DimenRes dimenRes: Int): Float {
    return resources.getDimension(dimenRes)
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun Context.makeToast(@StringRes res: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, res, duration).show()
}

fun Context.makeToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}


