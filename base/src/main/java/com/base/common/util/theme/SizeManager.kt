package com.base.common.util.theme

import android.content.Context
import android.content.res.Configuration
import com.base.common.base.LsApplication
import com.intuit.ssp.R
import com.base.common.extension.getDimens
import com.base.data.Prefs
import com.base.domain.model.DeviceType

object SizeManager {

    const val SIZE_TOOLBAR_LARGER = 0
    const val SIZE_TOOLBAR = 1
    const val SIZE_TITLE_LARGER = 2
    const val SIZE_TITLE = 3
    const val SIZE_PRIMARY_LARGER = 4
    const val SIZE_PRIMARY = 5
    const val SIZE_SECONDARY_LARGER = 6
    const val SIZE_SECONDARY = 7
    const val SIZE_TERTIARY_LARGER = 8
    const val SIZE_TERTIARY = 9
    const val SIZE_TERTIARY_SMALLER = 10

    fun Context.getDeviceType(isInEditMode: Boolean): DeviceType {
        return if (isInEditMode) {
            val metrics = resources.displayMetrics
            val widthDp = metrics.widthPixels / metrics.density
            when {
                widthDp >= 600 -> DeviceType.TABLET
                else -> DeviceType.PHONE
            }
        } else {
            val screenLayout = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            when {
                screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE -> DeviceType.TABLET
                else -> DeviceType.PHONE
            }
        }
    }

    fun sizeById(context: Context, isInEditMode: Boolean, textSize: Int, id: Int): Float {
        val deviceType = when {
            isInEditMode -> context.getDeviceType(true)
            else -> LsApplication.app.config.deviceType
        }

        return when (deviceType) {
            DeviceType.FOLDABLE -> getFoldSize(context, textSize, id)
            DeviceType.TABLET -> getTabletSize(context, textSize, id)
            DeviceType.PHONE -> getPhoneSize(context, textSize, id)
        }
    }

    private fun getPhoneSize(context: Context, textSize: Int, id: Int): Float {
        return when (textSize) {
            Prefs.TEXT_SIZE_SMALL -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._21ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._19ssp)
                SIZE_TITLE_LARGER -> context.getDimens(R.dimen._17ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._16ssp)
                SIZE_PRIMARY_LARGER -> context.getDimens(R.dimen._15ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._14ssp)
                SIZE_SECONDARY_LARGER -> context.getDimens(R.dimen._13ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._12ssp)
                SIZE_TERTIARY_LARGER -> context.getDimens(R.dimen._11ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._10ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._9ssp)
                else -> context.getDimens(R.dimen._14ssp)
            }
            Prefs.TEXT_SIZE_NORMAL -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._23ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._20ssp)
                SIZE_TITLE_LARGER -> context.getDimens(R.dimen._18ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._17ssp)
                SIZE_PRIMARY_LARGER -> context.getDimens(R.dimen._16ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._15ssp)
                SIZE_SECONDARY_LARGER -> context.getDimens(R.dimen._14ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._13ssp)
                SIZE_TERTIARY_LARGER -> context.getDimens(R.dimen._12ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._11ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._10ssp)
                else -> context.getDimens(R.dimen._15ssp)
            }
            Prefs.TEXT_SIZE_LARGE -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._23ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._21ssp)
                SIZE_TITLE_LARGER -> context.getDimens(R.dimen._19ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._18ssp)
                SIZE_PRIMARY_LARGER -> context.getDimens(R.dimen._17ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._16ssp)
                SIZE_SECONDARY_LARGER -> context.getDimens(R.dimen._15ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._14ssp)
                SIZE_TERTIARY_LARGER -> context.getDimens(R.dimen._13ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._12ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._11ssp)
                else -> context.getDimens(R.dimen._16ssp)
            }
            Prefs.TEXT_SIZE_LARGER -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._24ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._22ssp)
                SIZE_TITLE_LARGER -> context.getDimens(R.dimen._20ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._19ssp)
                SIZE_PRIMARY_LARGER -> context.getDimens(R.dimen._18ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._17ssp)
                SIZE_SECONDARY_LARGER -> context.getDimens(R.dimen._16ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._15ssp)
                SIZE_TERTIARY_LARGER -> context.getDimens(R.dimen._14ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._13ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._12ssp)
                else -> context.getDimens(R.dimen._17ssp)
            }
            else -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._25ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._23ssp)
                SIZE_TITLE_LARGER -> context.getDimens(R.dimen._21ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._20ssp)
                SIZE_PRIMARY_LARGER -> context.getDimens(R.dimen._19ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._18ssp)
                SIZE_SECONDARY_LARGER -> context.getDimens(R.dimen._17ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._16ssp)
                SIZE_TERTIARY_LARGER -> context.getDimens(R.dimen._15ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._14ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._13ssp)
                else -> context.getDimens(R.dimen._18ssp)
            }
        }
    }

    private fun getTabletSize(context: Context, textSize: Int, id: Int): Float {
        return when (textSize) {
            Prefs.TEXT_SIZE_SMALL -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._14ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._12ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._10ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._8ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._7ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._6ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._5ssp)
                else -> context.getDimens(R.dimen._8ssp)
            }
            Prefs.TEXT_SIZE_NORMAL -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._15ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._13ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._11ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._9ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._8ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._7ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._6ssp)
                else -> context.getDimens(R.dimen._9ssp)
            }
            Prefs.TEXT_SIZE_LARGE -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._16ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._14ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._12ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._10ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._9ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._8ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._7ssp)
                else -> context.getDimens(R.dimen._10ssp)
            }
            Prefs.TEXT_SIZE_LARGER -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._17ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._15ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._13ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._11ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._10ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._9ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._8ssp)
                else -> context.getDimens(R.dimen._11ssp)
            }
            else -> context.getDimens(R.dimen._11ssp)
        }
    }

    private fun getFoldSize(context: Context, textSize: Int, id: Int): Float {
        return when (textSize) {
            Prefs.TEXT_SIZE_SMALL -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._14ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._12ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._10ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._8ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._7ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._6ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._5ssp)
                else -> context.getDimens(R.dimen._8ssp)
            }
            Prefs.TEXT_SIZE_NORMAL -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._15ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._13ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._11ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._9ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._8ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._7ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._6ssp)
                else -> context.getDimens(R.dimen._9ssp)
            }
            Prefs.TEXT_SIZE_LARGE -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._16ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._14ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._12ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._10ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._9ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._8ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._7ssp)
                else -> context.getDimens(R.dimen._10ssp)
            }
            Prefs.TEXT_SIZE_LARGER -> when (id) {
                SIZE_TOOLBAR_LARGER -> context.getDimens(R.dimen._17ssp)
                SIZE_TOOLBAR -> context.getDimens(R.dimen._15ssp)
                SIZE_TITLE -> context.getDimens(R.dimen._13ssp)
                SIZE_PRIMARY -> context.getDimens(R.dimen._11ssp)
                SIZE_SECONDARY -> context.getDimens(R.dimen._10ssp)
                SIZE_TERTIARY -> context.getDimens(R.dimen._9ssp)
                SIZE_TERTIARY_SMALLER -> context.getDimens(R.dimen._8ssp)
                else -> context.getDimens(R.dimen._11ssp)
            }
            else -> context.getDimens(R.dimen._11ssp)
        }
    }

}