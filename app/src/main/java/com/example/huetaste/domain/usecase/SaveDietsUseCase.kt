package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class SaveDietsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(dietIds: List<String>) = repository.saveMyDiets(dietIds)
}
