package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.ScanHistoryFilter
import com.example.huetaste.domain.model.ScanHistoryPage
import com.example.huetaste.domain.model.ScanResult
import com.example.huetaste.domain.repository.ScanRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScanRepository {

    override suspend fun scan(image: File): ScanResult = withContext(ioDispatcher) {
        val part = MultipartBody.Part.createFormData(
            "file",
            image.name,
            image.asRequestBody("image/jpeg".toMediaType()),
        )
        apiCall(gson) { api.scan(part).toDomain() }
    }

    override suspend fun getScan(scanId: String): ScanResult = withContext(ioDispatcher) {
        apiCall(gson) { api.getScan(scanId).toDomain() }
    }

    override suspend fun getHistory(page: Int, filter: ScanHistoryFilter): ScanHistoryPage =
        withContext(ioDispatcher) {
            apiCall(gson) { api.getScanHistory(page, ScanHistoryPage.PAGE_SIZE, filter.apiValue).toDomain(page) }
        }

    override suspend fun setScanDeleted(scanId: String, deleted: Boolean) = withContext(ioDispatcher) {
        apiCall(gson) { if (deleted) api.deleteScan(scanId) else api.restoreScan(scanId) }
    }
}
