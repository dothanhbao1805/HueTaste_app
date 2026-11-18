package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.ScanHistoryFilter
import com.example.huetaste.domain.model.ScanHistoryPage
import com.example.huetaste.domain.model.ScanResult
import java.io.File

interface ScanRepository {
    /** Uploads a JPEG photo for recognition. */
    suspend fun scan(image: File): ScanResult

    suspend fun getScan(scanId: String): ScanResult

    suspend fun getHistory(page: Int, filter: ScanHistoryFilter): ScanHistoryPage

    /** Hides a scan from the history ([deleted] true) or brings it back (false, for "Hoàn tác"). */
    suspend fun setScanDeleted(scanId: String, deleted: Boolean)
}
