package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DishSearchItem
import com.example.huetaste.domain.model.DishSearchQuery
import com.example.huetaste.domain.model.SearchPage
import com.example.huetaste.domain.repository.DishRepository
import javax.inject.Inject

class SearchDishesUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(query: DishSearchQuery, page: Int): SearchPage<DishSearchItem> =
        repository.searchDishes(query, page)
}
