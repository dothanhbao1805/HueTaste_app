package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ScanResult
import com.example.huetaste.domain.repository.ScanRepository
import java.io.File
import javax.inject.Inject

class ScanDishUseCase @Inject constructor(private val repository: ScanRepository) {
    suspend operator fun invoke(image: File): ScanResult = repository.scan(image)
}
