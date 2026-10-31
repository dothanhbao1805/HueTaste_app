package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.repository.PlaceRepository
import javax.inject.Inject

class GetPlaceDetailUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(placeId: String): PlaceDetail = repository.getPlaceDetail(placeId)
}
