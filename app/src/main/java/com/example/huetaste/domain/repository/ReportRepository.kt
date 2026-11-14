package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.PlaceReport
import com.example.huetaste.domain.model.ReportPage

interface ReportRepository {
    /** Reports wrong information about a place; [dishIds] are dishes of its menu the user flags as wrong. */
    suspend fun submitReport(placeId: String, reason: String, description: String?, dishIds: List<String>): PlaceReport

    /** The signed-in user's reports, one page of [ReportPage.PAGE_SIZE] at a time. */
    suspend fun getMyReports(page: Int): ReportPage
}
