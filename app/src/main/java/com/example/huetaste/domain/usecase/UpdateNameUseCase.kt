package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateNameUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(name: String) = repository.updateName(name)
}
