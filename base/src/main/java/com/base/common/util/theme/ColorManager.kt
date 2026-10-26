package com.base.common.util.theme

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorRes
import com.base.R
import com.base.common.extension.getColorCompat
import com.base.common.extension.isDayOrNight
import com.base.common.extension.isNightMode
import com.base.data.Prefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorManager @Inject constructor(
    private val context: Context,
    private val prefs: Prefs
) {

    companion object {
        const val RIPPLE_NONE = -1
        const val RIPPLE = 0
        const val RIPPLE_BORDERLESS = 1

        const val COLOR_NONE = -1
        const val COLOR_THEME = 0
        const val COLOR_ON_THEME = 1
        const val COLOR_BACKGROUND = 2
        const val COLOR_CARD_BACKGROUND = 3
        const val COLOR_PRIMARY = 4
        const val COLOR_SECONDARY = 5
        const val COLOR_TERTIARY = 6
        private const val COLOR_ON_PRIMARY = 7
        private const val COLOR_ON_SECONDARY = 8
        private const val COLOR_ON_TERTIARY = 9
        const val COLOR_TEXT_PRIMARY = 10
        const val COLOR_TEXT_SECONDARY = 11
        const val COLOR_TEXT_TERTIARY = 12
        private const val COLOR_DIVIDER = 13
        private const val COLOR_RIPPLE = 14
        private const val COLOR_BLUEGRAY_800 = 15

        fun colorById(context: Context, id: Int, isNightMode: Boolean) = when (id) {
            COLOR_THEME -> context.getColorCompat(if (isNightMode) R.color.toolsThemeDark else R.color.toolsThemeLight)
            COLOR_ON_THEME -> context.getColorCompat(if (isNightMode) R.color.onToolsThemeDark else R.color.onToolsThemeLight)
            COLOR_BACKGROUND -> context.getColorCompat(if (isNightMode) R.color.backgroundDark else R.color.backgroundLight)
            COLOR_CARD_BACKGROUND -> context.getColorCompat(if (isNightMode) R.color.cardBackgroundDark else R.color.cardBackgroundLight)
            COLOR_PRIMARY -> context.getColorCompat(if (isNightMode) R.color.primaryDark else R.color.primaryLight)
            COLOR_SECONDARY -> context.getColorCompat(if (isNightMode) R.color.secondaryDark else R.color.secondaryLight)
            COLOR_TERTIARY -> context.getColorCompat(if (isNightMode) R.color.tertiaryDark else R.color.tertiaryLight)
            COLOR_ON_PRIMARY -> context.getColorCompat(if (isNightMode) R.color.onPrimaryDark else R.color.onPrimaryLight)
            COLOR_ON_SECONDARY -> context.getColorCompat(if (isNightMode) R.color.onSecondaryDark else R.color.onSecondaryLight)
            COLOR_ON_TERTIARY -> context.getColorCompat(if (isNightMode) R.color.onTertiaryDark else R.color.onTertiaryLight)
            COLOR_TEXT_PRIMARY -> context.getColorCompat(if (isNightMode) R.color.textPrimaryDark else R.color.textPrimaryLight)
            COLOR_TEXT_SECONDARY -> context.getColorCompat(if (isNightMode) R.color.textSecondaryDark else R.color.textSecondaryLight)
            COLOR_TEXT_TERTIARY -> context.getColorCompat(if (isNightMode) R.color.textTertiaryDark else R.color.textTertiaryLight)
            COLOR_DIVIDER -> context.getColorCompat(if (isNightMode) R.color.dividerDark else R.color.dividerLight)
            COLOR_RIPPLE -> context.getColorCompat(if (isNightMode) R.color.rippleDark else R.color.rippleLight)
            COLOR_BLUEGRAY_800 -> context.getColorCompat(if (isNightMode) R.color.blueGray800Dark else R.color.blueGray800Light)
            else -> null
        }
    }

    private var theme = Theme(prefs.themeId.get(), this)

    fun reload(themeId: Int = -1) {
        theme = when {
            themeId != -1 -> Theme(themeId, this)
            else -> Theme(prefs.themeId.get(), this)
        }
    }

    fun theme() = theme

    data class Theme(private val theme: Int, private val colorManager: ColorManager) {
        private val colors by lazy {
            mapOf(
                COLOR_NONE to null,
                COLOR_THEME to colorManager.toolsTheme(theme),
                COLOR_ON_THEME to colorManager.onToolsTheme(theme),
                COLOR_BACKGROUND to colorManager.background(theme),
                COLOR_CARD_BACKGROUND to colorManager.cardBackground(theme),
                COLOR_PRIMARY to colorManager.primary(theme),
                COLOR_SECONDARY to colorManager.secondary(theme),
                COLOR_TERTIARY to colorManager.tertiary(theme),
                COLOR_ON_PRIMARY to colorManager.onPrimary(theme),
                COLOR_ON_SECONDARY to colorManager.onSecondary(theme),
                COLOR_ON_TERTIARY to colorManager.onTertiary(theme),
                COLOR_TEXT_PRIMARY to colorManager.textPrimary(theme),
                COLOR_TEXT_SECONDARY to colorManager.textSecondary(theme),
                COLOR_TEXT_TERTIARY to colorManager.textTertiary(theme),
                COLOR_DIVIDER to colorManager.divider(theme),
                COLOR_RIPPLE to colorManager.ripple(theme),
                COLOR_BLUEGRAY_800 to colorManager.blueGray800(theme)
            )
        }

        val toolsTheme by lazy { colorManager.toolsTheme(theme) }
        val onToolsTheme by lazy { colorManager.onToolsTheme(theme) }
        val background by lazy { colorManager.background(theme) }
        val cardBackground by lazy { colorManager.cardBackground(theme) }
        val primary by lazy { colorManager.primary(theme) }
        val secondary by lazy { colorManager.secondary(theme) }
        val tertiary by lazy { colorManager.tertiary(theme) }
        val onPrimary by lazy { colorManager.onPrimary(theme) }
        val onSecondary by lazy { colorManager.onSecondary(theme) }
        val onTertiary by lazy { colorManager.onTertiary(theme) }
        val textPrimary by lazy { colorManager.textPrimary(theme) }
        val textSecondary by lazy { colorManager.textSecondary(theme) }
        val textTertiary by lazy { colorManager.textTertiary(theme) }
        val divider by lazy { colorManager.divider(theme) }
        val ripple by lazy { colorManager.ripple(theme) }
        val blueGray800 by lazy { colorManager.blueGray800(theme) }

        fun colorById(colorId: Int?) = colors[colorId]
    }

    private fun toolsTheme(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.toolsThemeLight)
        Prefs.NIGHT_MODE -> color(R.color.toolsThemeDark)
        else -> color(if (isDayOrNight()) R.color.toolsThemeLight else R.color.toolsThemeDark)
    }

    private fun onToolsTheme(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.onToolsThemeLight)
        Prefs.NIGHT_MODE -> color(R.color.onToolsThemeDark)
        else -> color(if (isDayOrNight()) R.color.onToolsThemeLight else R.color.onToolsThemeDark)
    }

    private fun primary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.primaryLight)
        Prefs.NIGHT_MODE -> color(R.color.primaryDark)
        else -> color(if (isDayOrNight()) R.color.primaryLight else R.color.primaryDark)
    }

    private fun secondary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.secondaryLight)
        Prefs.NIGHT_MODE -> color(R.color.secondaryDark)
        else -> color(if (isDayOrNight()) R.color.secondaryLight else R.color.secondaryDark)
    }

    private fun tertiary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.tertiaryLight)
        Prefs.NIGHT_MODE -> color(R.color.tertiaryDark)
        else -> color(if (isDayOrNight()) R.color.tertiaryLight else R.color.tertiaryDark)
    }

    private fun onPrimary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.onPrimaryLight)
        Prefs.NIGHT_MODE -> color(R.color.onPrimaryDark)
        else -> color(if (isDayOrNight()) R.color.onPrimaryLight else R.color.onPrimaryDark)
    }

    private fun onSecondary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.onSecondaryLight)
        Prefs.NIGHT_MODE -> color(R.color.onSecondaryDark)
        else -> color(if (isDayOrNight()) R.color.onSecondaryLight else R.color.onSecondaryDark)
    }

    private fun onTertiary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.onTertiaryLight)
        Prefs.NIGHT_MODE -> color(R.color.onTertiaryDark)
        else -> color(if (isDayOrNight()) R.color.onTertiaryLight else R.color.onTertiaryDark)
    }

    private fun background(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.backgroundLight)
        Prefs.NIGHT_MODE -> color(R.color.backgroundDark)
        else -> color(if (isDayOrNight()) R.color.backgroundLight else R.color.backgroundDark)
    }

    private fun cardBackground(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.cardBackgroundLight)
        Prefs.NIGHT_MODE -> color(R.color.cardBackgroundDark)
        else -> color(if (isDayOrNight()) R.color.cardBackgroundLight else R.color.cardBackgroundDark)
    }

    private fun textPrimary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.textPrimaryLight)
        Prefs.NIGHT_MODE -> color(R.color.textPrimaryDark)
        else -> color(if (isDayOrNight()) R.color.textPrimaryLight else R.color.textPrimaryDark)
    }

    private fun textSecondary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.textSecondaryLight)
        Prefs.NIGHT_MODE -> color(R.color.textSecondaryDark)
        else -> color(if (isDayOrNight()) R.color.textSecondaryLight else R.color.textSecondaryDark)
    }

    private fun textTertiary(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.textTertiaryLight)
        Prefs.NIGHT_MODE -> color(R.color.textTertiaryDark)
        else -> color(if (isDayOrNight()) R.color.textTertiaryLight else R.color.textTertiaryDark)
    }

    private fun divider(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.dividerLight)
        Prefs.NIGHT_MODE -> color(R.color.dividerDark)
        else -> color(if (isDayOrNight()) R.color.dividerLight else R.color.dividerDark)
    }

    private fun ripple(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.rippleLight)
        Prefs.NIGHT_MODE -> color(R.color.rippleDark)
        else -> color(if (isDayOrNight()) R.color.rippleLight else R.color.rippleDark)
    }

    private fun blueGray800(theme: Int) = when (theme) {
        Prefs.LIGHT_MODE -> color(R.color.blueGray800Light)
        Prefs.NIGHT_MODE -> color(R.color.blueGray800Dark)
        else -> color(if (isDayOrNight()) R.color.blueGray800Light else R.color.blueGray800Dark)
    }

    private fun color(@ColorRes colorRes: Int) = context.getColorCompat(colorRes)

    fun orange() = context.getColorCompat(R.color.color_orange)

    fun yellow() = context.getColorCompat(R.color.color_yellow)

    fun cardYellow() = context.getColorCompat(R.color.card_yellow)

    fun gray() = context.getColorCompat(R.color.card_gray)

    fun shimmer() = context.getColorCompat(R.color.color_shimmer)

}
