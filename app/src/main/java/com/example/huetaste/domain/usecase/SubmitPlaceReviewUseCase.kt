package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.repository.PlaceRepository
import javax.inject.Inject

class SubmitPlaceReviewUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(placeId: String, rating: Int, content: String?): Review =
        repository.submitReview(placeId, rating, content)
}
