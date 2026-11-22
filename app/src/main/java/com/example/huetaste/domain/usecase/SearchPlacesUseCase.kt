package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.PlaceSearchItem
import com.example.huetaste.domain.model.PlaceSearchQuery
import com.example.huetaste.domain.model.SearchPage
import com.example.huetaste.domain.repository.PlaceRepository
import javax.inject.Inject

class SearchPlacesUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(query: PlaceSearchQuery, page: Int): SearchPage<PlaceSearchItem> =
        repository.searchPlaces(query, page)
}
