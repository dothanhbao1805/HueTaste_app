package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.repository.PreferenceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Whether a user who just signed in should set up their taste: only when they have not finished or skipped it on
 * this device and the backend has neither diets nor tastes for them. A failed check never blocks the main screen.
 */
class ShouldShowTasteSetupUseCase @Inject constructor(private val repository: PreferenceRepository) {
    suspend operator fun invoke(): Boolean {
        if (repository.isSetupHandled()) return false
        return try {
            val empty = coroutineScope {
                val diets = async { repository.getMyDietIds() }
                val tastes = async { repository.getMyTastes() }
                diets.await().isEmpty() && tastes.await().isEmpty()
            }
            // Preferences set elsewhere count as a finished setup, which spares the next login the requests.
            if (!empty) repository.markSetupHandled()
            empty
        } catch (e: AppError) {
            false
        }
    }
}
