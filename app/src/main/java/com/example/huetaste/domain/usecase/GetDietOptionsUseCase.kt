package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class GetDietOptionsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(): List<DietOption> = repository.getDietOptions()
}
