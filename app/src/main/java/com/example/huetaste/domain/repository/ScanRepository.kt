package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.ScanResult
import java.io.File

interface ScanRepository {
    /** Uploads a JPEG photo for recognition. */
    suspend fun scan(image: File): ScanResult

    suspend fun getScan(scanId: String): ScanResult
}
