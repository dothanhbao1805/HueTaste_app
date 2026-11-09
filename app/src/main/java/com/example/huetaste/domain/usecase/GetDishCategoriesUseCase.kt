package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.repository.DishRepository
import javax.inject.Inject

class GetDishCategoriesUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(): List<DishCategory> = repository.getCategories()
}
