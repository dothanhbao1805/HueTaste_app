package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.VietDanhGiaBody
import com.example.huetaste.data.remote.dto.YeuThichDiaDiemBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.model.ReviewPage
import com.example.huetaste.domain.repository.PlaceRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaceRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlaceRepository {

    override suspend fun getPlaceDetail(placeId: String): PlaceDetail = withContext(ioDispatcher) {
        apiCall(gson) { api.getPlaceDetail(placeId).toDomain() }
    }

    override suspend fun getReviews(placeId: String, page: Int): ReviewPage = withContext(ioDispatcher) {
        apiCall(gson) { api.getPlaceReviews(placeId, page, ReviewPage.PAGE_SIZE).toDomain() }
    }

    override suspend fun submitReview(placeId: String, rating: Int, content: String?): Review =
        withContext(ioDispatcher) {
            apiCall(gson) {
                api.submitReview(VietDanhGiaBody(idDiaDiem = placeId, diemSo = rating, noiDung = content)).toDomain()
            }
        }

    override suspend fun setFavorite(placeId: String, favorite: Boolean) {
        withContext(ioDispatcher) {
            apiCall(gson) {
                if (favorite) api.addFavoritePlace(YeuThichDiaDiemBody(placeId)) else api.removeFavoritePlace(placeId)
            }
        }
    }
}
