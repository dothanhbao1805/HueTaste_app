package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.DishRepository
import javax.inject.Inject

class SetDishFavoriteUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(dishId: String, favorite: Boolean) = repository.setFavorite(dishId, favorite)
}
