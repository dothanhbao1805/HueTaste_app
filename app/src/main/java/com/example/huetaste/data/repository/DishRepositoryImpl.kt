package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.DishDetail
import com.example.huetaste.domain.repository.DishRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DishRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DishRepository {

    override suspend fun getDishDetail(dishId: String): DishDetail = withContext(ioDispatcher) {
        apiCall(gson) { api.getDishDetail(dishId).toDomain() }
    }
}
