package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class SearchIngredientsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(query: String): List<Ingredient> = repository.searchIngredients(query.trim())
}
