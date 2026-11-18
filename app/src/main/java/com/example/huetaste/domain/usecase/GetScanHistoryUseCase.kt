package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ScanHistoryFilter
import com.example.huetaste.domain.model.ScanHistoryPage
import com.example.huetaste.domain.repository.ScanRepository
import javax.inject.Inject

class GetScanHistoryUseCase @Inject constructor(private val repository: ScanRepository) {
    suspend operator fun invoke(page: Int, filter: ScanHistoryFilter): ScanHistoryPage =
        repository.getHistory(page, filter)
}
