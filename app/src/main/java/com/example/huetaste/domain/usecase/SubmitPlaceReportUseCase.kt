package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.PlaceReport
import com.example.huetaste.domain.repository.ReportRepository
import javax.inject.Inject

class SubmitPlaceReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(placeId: String, reason: String, description: String?, dishIds: List<String>): PlaceReport =
        repository.submitReport(placeId, reason, description, dishIds)
}
