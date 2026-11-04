package com.example.huetaste.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers, per user on this device, that the taste setup was finished or skipped so it is not offered again. */
@Singleton
class TasteSetupStore @Inject constructor(private val preferences: SharedPreferences) {
    fun isHandled(userId: String): Boolean = preferences.getBoolean(key(userId), false)

    fun markHandled(userId: String) {
        preferences.edit().putBoolean(key(userId), true).apply()
    }

    private fun key(userId: String) = "$KEY_PREFIX$userId"

    private companion object {
        const val KEY_PREFIX = "taste_setup_handled_"
    }
}
