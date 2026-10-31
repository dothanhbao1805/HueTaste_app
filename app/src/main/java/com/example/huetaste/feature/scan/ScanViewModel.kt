package com.example.huetaste.feature.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.ScanStatus
import com.example.huetaste.domain.usecase.GetScanResultUseCase
import com.example.huetaste.domain.usecase.ScanDishUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ScanUiState {
    data object Camera : ScanUiState

    /**
     * [imagePath] is the photo being identified, shown frozen behind the scan animation; it is null
     * while a camera capture or gallery pick is still being prepared.
     */
    data class Identifying(val imagePath: String?) : ScanUiState
}

sealed interface ScanEvent {
    data class OpenDetail(val scanId: String) : ScanEvent
    data class Failed(val error: AppError) : ScanEvent
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val scanDish: ScanDishUseCase,
    private val getScanResult: GetScanResultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Camera)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var identifyJob: Job? = null

    init {
        // After process death, only a scan the backend already accepted is resumed; an upload that
        // never got an id is not retried silently because it would spend another daily scan.
        val pendingId = savedState.get<String>(KEY_PENDING_SCAN_ID)
        val imagePath = savedState.get<String>(KEY_IMAGE_PATH)
        if (pendingId != null && imagePath != null) {
            _uiState.value = ScanUiState.Identifying(imagePath)
            identifyJob = viewModelScope.launch { waitForResult(pendingId) }
        } else {
            clearSavedScan()
        }
    }

    /** Locks the screen while a photo is captured or decoded; [identify] must follow. */
    fun startPreparing() {
        if (_uiState.value is ScanUiState.Camera) _uiState.value = ScanUiState.Identifying(null)
    }

    fun identify(image: File) {
        // The user may have cancelled while the photo was being prepared.
        if (_uiState.value !is ScanUiState.Identifying || identifyJob?.isActive == true) return
        savedState[KEY_IMAGE_PATH] = image.absolutePath
        _uiState.value = ScanUiState.Identifying(image.absolutePath)
        identifyJob = viewModelScope.launch {
            try {
                val result = scanDish(image)
                if (result.status == ScanStatus.UPDATING) {
                    savedState[KEY_PENDING_SCAN_ID] = result.id
                    waitForResult(result.id)
                } else {
                    finish(ScanEvent.OpenDetail(result.id))
                }
            } catch (e: AppError) {
                finish(ScanEvent.Failed(e))
            }
        }
    }

    /** Shows a capture or decoding failure that happened before anything reached the backend. */
    fun reportLocalFailure() {
        if (_uiState.value !is ScanUiState.Identifying) return
        identifyJob?.cancel()
        _uiState.value = ScanUiState.Camera
        finish(ScanEvent.Failed(AppError(CAPTURE_FAILED, null)))
    }

    fun cancel() {
        identifyJob?.cancel()
        identifyJob = null
        clearSavedScan()
        _uiState.value = ScanUiState.Camera
    }

    /** Called once the error dialog is answered with "retake". */
    fun resetToCamera() {
        _uiState.value = ScanUiState.Camera
    }

    private suspend fun waitForResult(scanId: String) {
        // The backend researches a new Huế dish in the background and settles within ~120 s.
        val deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            delay(POLL_INTERVAL_MS)
            val status = try {
                getScanResult(scanId).status
            } catch (e: AppError) {
                break
            }
            if (status != ScanStatus.UPDATING) break
        }
        // The detail screen reloads the scan and shows whatever state it reached.
        finish(ScanEvent.OpenDetail(scanId))
    }

    private fun finish(event: ScanEvent) {
        clearSavedScan()
        _events.trySend(event)
    }

    private fun clearSavedScan() {
        savedState.remove<String>(KEY_PENDING_SCAN_ID)
        savedState.remove<String>(KEY_IMAGE_PATH)
    }

    companion object {
        const val CAPTURE_FAILED = "CAPTURE_FAILED"
        private const val KEY_PENDING_SCAN_ID = "pending_scan_id"
        private const val KEY_IMAGE_PATH = "image_path"
        private const val POLL_INTERVAL_MS = 3_000L
        private const val POLL_TIMEOUT_MS = 130_000L
    }
}
