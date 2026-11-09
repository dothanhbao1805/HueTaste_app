package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.repository.DishRepository
import javax.inject.Inject

class GetDishesUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(size: Int): List<Dish> = repository.getDishes(size)
}
