package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DishDetail
import com.example.huetaste.domain.repository.DishRepository
import javax.inject.Inject

class GetDishDetailUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(dishId: String): DishDetail = repository.getDishDetail(dishId)
}
