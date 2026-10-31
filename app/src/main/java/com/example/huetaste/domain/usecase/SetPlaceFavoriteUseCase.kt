package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.PlaceRepository
import javax.inject.Inject

class SetPlaceFavoriteUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(placeId: String, favorite: Boolean) = repository.setFavorite(placeId, favorite)
}
