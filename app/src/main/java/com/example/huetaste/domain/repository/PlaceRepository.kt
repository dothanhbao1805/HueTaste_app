package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.HomePlace
import com.example.huetaste.domain.model.PlaceSearchItem
import com.example.huetaste.domain.model.PlaceSearchQuery
import com.example.huetaste.domain.model.SearchPage
import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.model.ReviewPage

interface PlaceRepository {
    /** At most [size] places within [radiusKm] of [point], nearest first. */
    suspend fun getNearbyPlaces(point: GeoPoint, radiusKm: Double, size: Int): List<HomePlace>

    suspend fun getFeaturedPlaces(size: Int): List<HomePlace>

    suspend fun searchPlaces(query: PlaceSearchQuery, page: Int): SearchPage<PlaceSearchItem>

    suspend fun getPlaceDetail(placeId: String): PlaceDetail

    suspend fun getReviews(placeId: String, page: Int): ReviewPage

    /** Creates the user's review, or replaces the one they already wrote for this place. */
    suspend fun submitReview(placeId: String, rating: Int, content: String?): Review

    suspend fun setFavorite(placeId: String, favorite: Boolean)
}
