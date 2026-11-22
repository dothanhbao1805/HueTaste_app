package com.example.huetaste.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** The last search terms typed on this device, newest first. */
@Singleton
class RecentSearchStore @Inject constructor(private val preferences: SharedPreferences) {
    fun get(): List<String> =
        preferences.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    /** Moves [term] to the front; the same term with other casing counts as one. */
    fun add(term: String) {
        val clean = term.trim().replace(SEPARATOR, " ")
        if (clean.isEmpty()) return
        save(listOf(clean) + get().filterNot { it.equals(clean, ignoreCase = true) })
    }

    fun remove(term: String) = save(get().filterNot { it == term })

    fun clear() {
        preferences.edit().remove(KEY).apply()
    }

    private fun save(terms: List<String>) {
        preferences.edit().putString(KEY, terms.take(MAX_TERMS).joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val KEY = "recent_searches"
        const val SEPARATOR = "\n"
        const val MAX_TERMS = 8
    }
}
