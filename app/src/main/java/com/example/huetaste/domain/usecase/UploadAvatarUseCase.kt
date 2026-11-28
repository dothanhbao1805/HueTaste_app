package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.AuthRepository
import java.io.File
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(image: File) = repository.uploadAvatar(image)
}
