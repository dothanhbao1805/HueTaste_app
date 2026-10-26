package com.base.data

import android.content.Context
import com.base.R
import com.f2prateek.rx.preferences2.RxSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Prefs @Inject constructor(
    private val context: Context,
    private val rxPrefs: RxSharedPreferences
) {

    companion object {
        const val NIGHT_MODE = 0
        const val LIGHT_MODE = 1
        const val AUTO_MODE = 2

        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_NORMAL = 1
        const val TEXT_SIZE_LARGE = 2
        const val TEXT_SIZE_LARGER = 3
        const val READING_SLIDE= true
        const val READING_SCROLL= false
    }

    // Config
    val language = rxPrefs.getString("language", "en")
//    val isUpgraded = rxPrefs.getBoolean("isUpgraded", false)
    val isAcceptedTutorial = rxPrefs.getBoolean("isAcceptedTutorial", false)

    val isConfigLanguage = rxPrefs.getBoolean("isConfigLanguage", false)
    /** Set once the location prompt has been shown, so launching does not nag. */
    val hasAskedLocation = rxPrefs.getBoolean("hasAskedLocation", false)
    val textSize = rxPrefs.getInteger("textSize", TEXT_SIZE_NORMAL)
    val themeId = rxPrefs.getInteger("themeId", LIGHT_MODE)
    val isVibrate = rxPrefs.getBoolean("isVibrate", true)

    val isFirsListBookRecommend = rxPrefs.getBoolean("isFirsListBookRecommend", false)
    val voiceParameter = rxPrefs.getString("voiceParameter", "")
    val playbackSpeed = rxPrefs.getFloat("playbackSpeed", 1.0F)
    val isFirstIntroduceVoice = rxPrefs.getBoolean("isFirstIntroduceVoice", true)

    val readingMode = rxPrefs.getBoolean("readingMode", READING_SLIDE)
    val currentFontSize = rxPrefs.getInteger("currentFontSize", 20)
    val backgroundMode= rxPrefs.getInteger("backgroundMode", 2)
    val readingModeType = rxPrefs.getInteger("readingModeType", 1)
    val fontTextId= rxPrefs.getInteger("fontTextId", R.font.times_new_roman_bold)
    val spacingText= rxPrefs.getFloat("spacingText", 7.toFloat())




    fun isNightMode() = themeId.get() == NIGHT_MODE
}
