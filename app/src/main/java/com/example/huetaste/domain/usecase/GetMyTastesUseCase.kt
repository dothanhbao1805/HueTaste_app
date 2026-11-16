package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.TastePreference
import com.example.huetaste.domain.repository.PreferenceRepository
import javax.inject.Inject

class GetMyTastesUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(): List<TastePreference> = repository.getMyTastes()
}
