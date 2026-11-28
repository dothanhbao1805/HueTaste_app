package com.example.huetaste.feature.language

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.huetaste.R

/** The languages the app can be switched to; [languageCode] is the BCP 47 tag given to the app locale. */
enum class AppLanguage(
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val flagRes: Int,
    val languageCode: String,
) {
    VIETNAMESE(R.string.language_vietnamese, R.drawable.ic_vietnam, "vi"),
    ENGLISH(R.string.language_english, R.drawable.ic_england, "en"),
    FRENCH(R.string.language_french, R.drawable.ic_france, "fr"),
    JAPANESE(R.string.language_japanese, R.drawable.ic_japan, "ja"),
    KOREAN(R.string.language_korean, R.drawable.ic_south_korea, "ko"),
    PORTUGUESE(R.string.language_portuguese, R.drawable.ic_portugal, "pt"),
    RUSSIAN(R.string.language_russian, R.drawable.ic_russia, "ru"),
    CHINESE(R.string.language_chinese, R.drawable.ic_china, "zh"),
    SPANISH(R.string.language_spanish, R.drawable.ic_spain, "es"),
    THAI(R.string.language_thai, R.drawable.ic_thailand, "th"),
    GERMAN(R.string.language_german, R.drawable.ic_germany, "de"),
    ITALIAN(R.string.language_italian, R.drawable.ic_italy, "it"),
    HINDI(R.string.language_hindi, R.drawable.ic_india, "hi"),
    ARABIC(R.string.language_arabic, R.drawable.ic_united_arab_emirates, "ar");

    companion object {
        /** The app is written in Vietnamese, so an unknown code falls back to it. */
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.languageCode == code } ?: VIETNAMESE
    }
}
