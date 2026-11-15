package com.example.huetaste.feature.my_reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.PlaceReport
import com.example.huetaste.domain.usecase.GetMyReportsUseCase
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

data class MyReportsUiState(
    val reports: List<PlaceReport> = emptyList(),
    val isLoading: Boolean = true,
    /** The first page failed; the screen shows the error with "Thử lại". */
    val error: AppError? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
)

sealed interface MyReportsEvent {
    object LoadMoreFailed : MyReportsEvent
}

/** Pages through `GET /users/me/bao-cao` (newest first). */
@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val getMyReports: GetMyReportsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyReportsUiState())
    val uiState: StateFlow<MyReportsUiState> = _uiState.asStateFlow()

    private val _events = Channel<MyReportsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null, isLoadingMore = false) }
        loadJob = viewModelScope.launch {
            try {
                val page = getMyReports(1)
                nextPage = 2
                _uiState.update { MyReportsUiState(reports = page.reports, isLoading = false, hasMore = page.hasMore) }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoading = false, error = e) }
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
                val page = getMyReports(nextPage)
                nextPage++
                _uiState.update { current ->
                    val known = current.reports.map { it.id }.toSet()
                    current.copy(
                        // A report sent meanwhile shifts the pages; skip the ones already shown.
                        reports = current.reports + page.reports.filterNot { it.id in known },
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _events.send(MyReportsEvent.LoadMoreFailed)
            }
        }
    }
}
