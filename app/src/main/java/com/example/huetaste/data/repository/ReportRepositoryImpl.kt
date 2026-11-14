package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.GuiBaoCaoBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.PlaceReport
import com.example.huetaste.domain.model.ReportPage
import com.example.huetaste.domain.repository.ReportRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ReportRepository {

    override suspend fun submitReport(
        placeId: String,
        reason: String,
        description: String?,
        dishIds: List<String>,
    ): PlaceReport = withContext(ioDispatcher) {
        apiCall(gson) {
            api.submitReport(
                GuiBaoCaoBody(idDiaDiem = placeId, lyDo = reason, moTa = description, idMonAn = dishIds),
            ).toDomain()
        }
    }

    override suspend fun getMyReports(page: Int): ReportPage = withContext(ioDispatcher) {
        apiCall(gson) { api.getMyReports(page, ReportPage.PAGE_SIZE).toDomain() }
    }
}
