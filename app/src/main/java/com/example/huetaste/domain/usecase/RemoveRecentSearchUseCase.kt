package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.RecentSearchRepository
import javax.inject.Inject

class RemoveRecentSearchUseCase @Inject constructor(private val repository: RecentSearchRepository) {
    suspend operator fun invoke(term: String) = repository.remove(term)
}
