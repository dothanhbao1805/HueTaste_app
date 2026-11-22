package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.mapper.toHomePlace
import com.example.huetaste.data.mapper.toSearchItem
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.VietDanhGiaBody
import com.example.huetaste.data.remote.dto.YeuThichDiaDiemBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.HomePlace
import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.PlaceSearchItem
import com.example.huetaste.domain.model.PlaceSearchQuery
import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.model.ReviewPage
import com.example.huetaste.domain.model.SearchPage
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

    override suspend fun getNearbyPlaces(point: GeoPoint, radiusKm: Double, size: Int): List<HomePlace> =
        withContext(ioDispatcher) {
            apiCall(gson) {
                api.getNearbyPlaces(point.latitude, point.longitude, radiusKm, size).items.orEmpty().map { it.toHomePlace() }
            }
        }

    override suspend fun getFeaturedPlaces(size: Int): List<HomePlace> = withContext(ioDispatcher) {
        apiCall(gson) { api.getFeaturedPlaces(size).items.orEmpty().map { it.toHomePlace() } }
    }

    override suspend fun searchPlaces(query: PlaceSearchQuery, page: Int): SearchPage<PlaceSearchItem> =
        withContext(ioDispatcher) {
            apiCall(gson) {
                val near = query.near
                val result = api.searchPlaces(
                    query = query.text.trim().ifEmpty { null },
                    latitude = near?.latitude,
                    longitude = near?.longitude,
                    radiusKm = near?.let { query.radiusKm },
                    area = query.area?.code,
                    page = page,
                    size = SearchPage.PAGE_SIZE,
                )
                val items = result.items.orEmpty().map { it.toSearchItem() }
                SearchPage(items = items, total = result.total ?: items.size, page = page)
            }
        }

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
