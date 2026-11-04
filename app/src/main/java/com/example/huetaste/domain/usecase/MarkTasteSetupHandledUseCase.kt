package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class MarkTasteSetupHandledUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke() = repository.markSetupHandled()
}
