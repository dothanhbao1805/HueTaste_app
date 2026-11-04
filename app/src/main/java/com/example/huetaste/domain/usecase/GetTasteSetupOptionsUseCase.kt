package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.TasteSetupOptions
import com.example.huetaste.domain.repository.PreferenceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetTasteSetupOptionsUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(): TasteSetupOptions = coroutineScope {
        val diets = async { repository.getDietOptions() }
        val tags = async { repository.getTasteTags() }
        TasteSetupOptions(diets.await(), tags.await())
    }
}
