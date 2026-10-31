package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ReviewPage
import com.example.huetaste.domain.repository.PlaceRepository
import javax.inject.Inject

class GetPlaceReviewsUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(placeId: String, page: Int): ReviewPage = repository.getReviews(placeId, page)
}
