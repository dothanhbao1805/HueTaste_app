package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.DishRepository
import javax.inject.Inject

class GetFavoriteDishIdsUseCase @Inject constructor(private val repository: DishRepository) {
    suspend operator fun invoke(): Set<String> = repository.getFavoriteDishIds()
}
