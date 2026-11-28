package com.example.huetaste.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers which notifications already went to the status bar, so the periodic check never repeats one. */
@Singleton
class ShownNotificationStore @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    /** Keeps the ids not seen before and records them; disk work, so call it off the main thread. */
    fun claimNew(ids: List<String>): Set<String> = synchronized(lock) {
        val shown = preferences.getString(KEY_IDS, null)?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
        val fresh = ids.filterNot { it in shown }.toSet()
        if (fresh.isNotEmpty()) {
            // Only the newest ids matter: older notifications are read or off the first page by now.
            val kept = (shown + fresh).takeLast(MAX_IDS)
            preferences.edit().putString(KEY_IDS, kept.joinToString(SEPARATOR)).apply()
        }
        fresh
    }

    /** True only the first time, so the notification permission is asked for once. */
    fun claimPermissionPrompt(): Boolean = synchronized(lock) {
        val first = !preferences.getBoolean(KEY_PROMPT_CLAIMED, false)
        if (first) preferences.edit().putBoolean(KEY_PROMPT_CLAIMED, true).apply()
        first
    }

    private companion object {
        const val FILE_NAME = "shown_notifications"
        const val KEY_PROMPT_CLAIMED = "permission_prompt_claimed"
        const val KEY_IDS = "ids"
        const val SEPARATOR = ","
        const val MAX_IDS = 100
    }
}
