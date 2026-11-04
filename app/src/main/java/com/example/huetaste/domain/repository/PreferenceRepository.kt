package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.model.TastePreference
import com.example.huetaste.domain.model.TasteTag

/** The signed-in user's diets, liked flavours and daily meal needs, plus the catalogs they are picked from. */
interface PreferenceRepository {
    suspend fun getDietOptions(): List<DietOption>
    suspend fun getTasteTags(): List<TasteTag>
    suspend fun searchIngredients(query: String): List<Ingredient>

    suspend fun getMyDietIds(): List<String>

    /** Replaces the user's whole diet list; an empty list means no diet. */
    suspend fun saveMyDiets(dietIds: List<String>)
    suspend fun getMyTastes(): List<TastePreference>

    /** Replaces the user's whole taste list. */
    suspend fun saveMyTastes(tastes: List<TastePreference>)

    /** Creates or overwrites today's need for [DailyNeed.meal]. */
    suspend fun saveDailyNeed(need: DailyNeed)

    /** Whether the current user finished or skipped the taste setup on this device. */
    suspend fun isSetupHandled(): Boolean
    suspend fun markSetupHandled()
}
