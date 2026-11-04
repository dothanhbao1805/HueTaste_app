package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.TastePreference
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class SaveTastesUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(tastes: List<TastePreference>) = repository.saveMyTastes(tastes)
}
