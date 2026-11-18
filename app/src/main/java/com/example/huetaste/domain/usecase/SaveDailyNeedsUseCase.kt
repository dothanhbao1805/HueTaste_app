package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

/**
 * Saves one need per meal; the backend has no batch endpoint, so the meals are sent one after another.
 * Meals of [replacing] (what was saved before) that are no longer in [needs] are deleted.
 */
class SaveDailyNeedsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(needs: List<DailyNeed>, replacing: List<DailyNeed> = emptyList()) {
        needs.forEach { repository.saveDailyNeed(it) }
        val kept = needs.map { it.meal }.toSet()
        replacing.filter { it.meal !in kept }.mapNotNull { it.id }.forEach { repository.deleteDailyNeed(it) }
    }
}
