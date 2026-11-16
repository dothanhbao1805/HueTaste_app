package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class GetMyDietIdsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(): List<String> = repository.getMyDietIds()
}
