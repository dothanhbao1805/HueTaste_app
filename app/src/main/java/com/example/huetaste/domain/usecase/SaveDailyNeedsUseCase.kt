package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

/** Saves one need per meal; the backend has no batch endpoint, so the meals are sent one after another. */
class SaveDailyNeedsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(needs: List<DailyNeed>) {
        needs.forEach { repository.saveDailyNeed(it) }
    }
}
