package com.example.huetaste.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.base.common.base.LsApplication
import com.base.common.util.theme.UIManager
import com.base.data.Prefs
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : LsApplication() {
    override fun onCreated() {
        UIManager.themeId = prefs.themeId.get()
        AppCompatDelegate.setDefaultNightMode(
            when (prefs.themeId.get()) {
                Prefs.NIGHT_MODE -> AppCompatDelegate.MODE_NIGHT_YES
                Prefs.LIGHT_MODE -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        // Only a language the user picked; the base default "en" would override the app's Vietnamese.
        if (prefs.language.isSet) {
            val saved = LocaleListCompat.forLanguageTags(prefs.language.get())
            if (AppCompatDelegate.getApplicationLocales() != saved) AppCompatDelegate.setApplicationLocales(saved)
        }
    }
}
