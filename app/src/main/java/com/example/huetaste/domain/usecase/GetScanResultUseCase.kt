package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ScanResult
import com.example.huetaste.domain.repository.ScanRepository
import javax.inject.Inject

class GetScanResultUseCase @Inject constructor(private val repository: ScanRepository) {
    suspend operator fun invoke(scanId: String): ScanResult = repository.getScan(scanId)
}
