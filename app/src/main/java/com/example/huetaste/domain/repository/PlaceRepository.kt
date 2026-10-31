package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.model.ReviewPage

interface PlaceRepository {
    suspend fun getPlaceDetail(placeId: String): PlaceDetail

    suspend fun getReviews(placeId: String, page: Int): ReviewPage

    /** Creates the user's review, or replaces the one they already wrote for this place. */
    suspend fun submitReview(placeId: String, rating: Int, content: String?): Review

    suspend fun setFavorite(placeId: String, favorite: Boolean)
}
