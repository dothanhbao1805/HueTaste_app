package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.SuggestionRequest
import com.example.huetaste.domain.model.SuggestionResult

interface SuggestionRepository {
    /** Only answers signed-in users. */
    suspend fun getSuggestions(request: SuggestionRequest): SuggestionResult
}
