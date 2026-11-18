package com.example.huetaste.feature.scan_history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.ScanHistoryFilter
import com.example.huetaste.domain.model.ScanHistoryItem
import com.example.huetaste.domain.usecase.GetScanHistoryUseCase
import com.example.huetaste.domain.usecase.SetScanDeletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanHistoryUiState(
    val filter: ScanHistoryFilter = ScanHistoryFilter.ALL,
    val items: List<ScanHistoryItem> = emptyList(),
    /** Scans matching [filter] on the backend, including pages not loaded yet. */
    val total: Int = 0,
    val isLoading: Boolean = true,
    /** The first page failed; the screen shows the error with "Thử lại". */
    val error: AppError? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

sealed interface ScanHistoryEvent {
    data object LoadFailed : ScanHistoryEvent
    data class Deleted(val scanId: String) : ScanHistoryEvent
    data object ActionFailed : ScanHistoryEvent
}

/** Pages through `GET /users/me/lich-su-scan` (newest first) and removes or restores scans. */
@HiltViewModel
class ScanHistoryViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getScanHistory: GetScanHistoryUseCase,
    private val setScanDeleted: SetScanDeletedUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScanHistoryUiState(filter = savedState.get<String>(KEY_FILTER)?.let(ScanHistoryFilter::valueOf) ?: ScanHistoryFilter.ALL),
    )
    val uiState: StateFlow<ScanHistoryUiState> = _uiState.asStateFlow()

    private val _events = Channel<ScanHistoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        reload()
    }

    fun selectFilter(filter: ScanHistoryFilter) {
        if (filter == _uiState.value.filter) return
        savedState[KEY_FILTER] = filter.name
        _uiState.update { it.copy(filter = filter, items = emptyList()) }
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        val filter = _uiState.value.filter
        _uiState.update { it.copy(isLoading = it.items.isEmpty(), error = null, isLoadingMore = false) }
        loadJob = viewModelScope.launch {
            try {
                val page = getScanHistory(1, filter)
                nextPage = 2
                _uiState.update {
                    ScanHistoryUiState(filter, page.items, page.total, isLoading = false, hasMore = page.hasMore)
                }
            } catch (e: AppError) {
                // A refresh over a list already shown keeps it and only reports the failure.
                val keepList = _uiState.value.items.isNotEmpty()
                _uiState.update { it.copy(isLoading = false, error = if (keepList) null else e) }
                if (keepList) _events.send(ScanHistoryEvent.LoadFailed)
            }
        }
    }

    /** Called when the list nears its end. */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.error != null) return
        _uiState.update { it.copy(isLoadingMore = true) }
        loadJob = viewModelScope.launch {
            try {
                val page = getScanHistory(nextPage, state.filter)
                nextPage++
                _uiState.update { current ->
                    val known = current.items.map { it.id }.toSet()
                    current.copy(
                        // A scan made or removed meanwhile shifts the pages; skip the ones already shown.
                        items = current.items + page.items.filterNot { it.id in known },
                        total = page.total,
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _events.send(ScanHistoryEvent.LoadFailed)
            }
        }
    }

    fun delete(scanId: String) {
        viewModelScope.launch {
            try {
                setScanDeleted(scanId, deleted = true)
                _uiState.update { state ->
                    state.copy(items = state.items.filterNot { it.id == scanId }, total = (state.total - 1).coerceAtLeast(0))
                }
                _events.send(ScanHistoryEvent.Deleted(scanId))
            } catch (e: AppError) {
                _events.send(ScanHistoryEvent.ActionFailed)
            }
        }
    }

    /** "Hoàn tác" after a delete: the scan comes back at its place in the newest-first list. */
    fun undoDelete(scanId: String) {
        viewModelScope.launch {
            try {
                setScanDeleted(scanId, deleted = false)
                reload()
            } catch (e: AppError) {
                _events.send(ScanHistoryEvent.ActionFailed)
            }
        }
    }

    private companion object {
        const val KEY_FILTER = "scan_history_filter"
    }
}
