package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.SuggestionRequest
import com.example.huetaste.domain.model.SuggestionResult
import com.example.huetaste.domain.repository.SuggestionRepository
import javax.inject.Inject

class GetDishSuggestionsUseCase @Inject constructor(private val repository: SuggestionRepository) {
    suspend operator fun invoke(request: SuggestionRequest): SuggestionResult = repository.getSuggestions(request)
}
