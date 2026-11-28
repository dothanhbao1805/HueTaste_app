package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    /** The current user, then every change to them, such as a new name or photo. */
    fun observe(): Flow<UserProfile?> = repository.observeUser()
}
