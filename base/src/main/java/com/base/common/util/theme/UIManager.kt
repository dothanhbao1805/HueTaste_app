package com.base.common.util.theme

import com.base.data.Prefs

object UIManager {

    private val listeners = mutableSetOf<UIChangedListener>()

    var themeId = Prefs.NIGHT_MODE
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) listeners.forEach { it.onThemeChanged(false) }
        }

    var language = "en"
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) listeners.forEach { it.onLanguageChanged(false) }
        }

    fun addListener(listener: UIChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: UIChangedListener) {
        listeners.remove(listener)
    }
}