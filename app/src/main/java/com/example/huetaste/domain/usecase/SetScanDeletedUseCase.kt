package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.repository.ScanRepository
import javax.inject.Inject

/** Removes a scan from the history, or restores it when the user taps "Hoàn tác". */
class SetScanDeletedUseCase @Inject constructor(private val repository: ScanRepository) {
    suspend operator fun invoke(scanId: String, deleted: Boolean) = repository.setScanDeleted(scanId, deleted)
}
