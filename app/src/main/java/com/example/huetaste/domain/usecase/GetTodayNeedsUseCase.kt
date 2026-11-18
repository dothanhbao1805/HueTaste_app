package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class GetTodayNeedsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(): List<DailyNeed> = repository.getTodayNeeds()
}
