package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.GoiYBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.SuggestionRequest
import com.example.huetaste.domain.model.SuggestionResult
import com.example.huetaste.domain.repository.SuggestionRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SuggestionRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SuggestionRepository {

    override suspend fun getSuggestions(request: SuggestionRequest): SuggestionResult = withContext(ioDispatcher) {
        apiCall(gson) {
            api.getSuggestions(
                GoiYBody(
                    buaAn = request.meal?.apiValue,
                    nganSach = request.budget,
                    ghiChu = request.note,
                    soMon = DISH_COUNT,
                    soDiaDiem = request.placeCount,
                    viDo = request.point?.latitude,
                    kinhDo = request.point?.longitude,
                ),
            ).toDomain()
        }
    }

    private companion object {
        const val DISH_COUNT = 10
    }
}
