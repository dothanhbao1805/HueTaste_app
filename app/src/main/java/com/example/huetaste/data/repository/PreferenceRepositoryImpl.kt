package com.example.huetaste.data.repository

import com.example.huetaste.data.local.SecureSessionStore
import com.example.huetaste.data.local.TasteSetupStore
import com.example.huetaste.data.mapper.toBody
import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.mapper.toDomainOrNull
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.CapNhatCheDoAnBody
import com.example.huetaste.data.remote.dto.CapNhatSoThichBody
import com.example.huetaste.data.remote.dto.CheDoAnNguoiDungBody
import com.example.huetaste.data.remote.dto.SoThichNguoiDungBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.model.TastePreference
import com.example.huetaste.domain.model.TasteTag
import com.example.huetaste.domain.repository.PreferenceRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferenceRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val sessionStore: SecureSessionStore,
    private val setupStore: TasteSetupStore,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PreferenceRepository {

    override suspend fun getDietOptions(): List<DietOption> = withContext(ioDispatcher) {
        apiCall(gson) { api.getDietOptions().map { it.toDomain() } }
    }

    override suspend fun getTasteTags(): List<TasteTag> = withContext(ioDispatcher) {
        apiCall(gson) { api.getTasteTags().mapNotNull { it.toDomainOrNull() } }
    }

    override suspend fun searchIngredients(query: String): List<Ingredient> = withContext(ioDispatcher) {
        apiCall(gson) { api.searchIngredients(query.ifBlank { null }, INGREDIENT_PAGE_SIZE).map { it.toDomain() } }
    }

    override suspend fun getMyDietIds(): List<String> = withContext(ioDispatcher) {
        apiCall(gson) { api.getMyDiets().map { it.idCheDoAn } }
    }

    override suspend fun saveMyDiets(dietIds: List<String>) = withContext(ioDispatcher) {
        apiCall(gson) { api.saveMyDiets(CapNhatCheDoAnBody(dietIds.map(::CheDoAnNguoiDungBody))) }
    }

    override suspend fun getMyTastes(): List<TastePreference> = withContext(ioDispatcher) {
        apiCall(gson) { api.getMyTastes().map { TastePreference(it.idSoThich, it.mucDo ?: DEFAULT_LEVEL) } }
    }

    override suspend fun saveMyTastes(tastes: List<TastePreference>) = withContext(ioDispatcher) {
        apiCall(gson) { api.saveMyTastes(CapNhatSoThichBody(tastes.map { SoThichNguoiDungBody(it.tagId, it.level) })) }
    }

    override suspend fun saveDailyNeed(need: DailyNeed) = withContext(ioDispatcher) {
        apiCall(gson) { api.saveDailyNeed(need.toBody()) }
    }

    // Reading the session decrypts with the Keystore, so the flag is read off the main thread too.
    override suspend fun isSetupHandled(): Boolean = withContext(ioDispatcher) {
        val userId = sessionStore.session()?.user?.id ?: return@withContext true
        setupStore.isHandled(userId)
    }

    override suspend fun markSetupHandled() = withContext(ioDispatcher) {
        sessionStore.session()?.user?.id?.let(setupStore::markHandled) ?: Unit
    }

    private companion object {
        const val INGREDIENT_PAGE_SIZE = 30
        const val DEFAULT_LEVEL = 3
    }
}
