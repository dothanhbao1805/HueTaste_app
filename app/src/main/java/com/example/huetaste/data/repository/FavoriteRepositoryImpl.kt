package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.Favorites
import com.example.huetaste.domain.repository.FavoriteRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FavoriteRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FavoriteRepository {

    override suspend fun getFavorites(): Favorites = withContext(ioDispatcher) {
        apiCall(gson) {
            val result = api.getFavorites()
            Favorites(
                dishes = result.monAn.orEmpty().map { it.toDomain() },
                places = result.diaDiem.orEmpty().map { it.toDomain() },
            )
        }
    }
}
