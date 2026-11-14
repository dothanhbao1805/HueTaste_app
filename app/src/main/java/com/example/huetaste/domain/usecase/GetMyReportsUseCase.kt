package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.ReportPage
import com.example.huetaste.domain.repository.ReportRepository
import javax.inject.Inject

/** The signed-in user's reports and how they were handled; for the "Báo cáo của tôi" screen. */
class GetMyReportsUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(page: Int): ReportPage = repository.getMyReports(page)
}
