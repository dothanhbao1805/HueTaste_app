package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.RecentSearchRepository
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(private val repository: RecentSearchRepository) {
    suspend operator fun invoke(): List<String> = repository.get()
}
