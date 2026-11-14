package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.Favorites
import com.example.huetaste.domain.repository.FavoriteRepository
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(private val repository: FavoriteRepository) {
    suspend operator fun invoke(): Favorites = repository.getFavorites()
}
