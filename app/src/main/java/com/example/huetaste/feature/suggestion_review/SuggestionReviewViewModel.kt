package com.example.huetaste.feature.suggestion_review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.common.navigation.SuggestionArgs
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.usecase.GetMyDietIdsUseCase
import com.example.huetaste.domain.usecase.GetMyTastesUseCase
import com.example.huetaste.domain.usecase.GetTasteSetupOptionsUseCase
import com.example.huetaste.domain.usecase.GetTodayNeedsUseCase
import com.example.huetaste.feature.taste_setup.OptionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The saved data the backend scores suggestions with; [tastes] are names, most liked first. */
data class SuggestionProfile(
    val tastes: List<String>,
    val diets: List<String>,
    val needs: List<DailyNeed>,
)

data class SuggestionReviewUiState(
    val args: SuggestionArgs,
    val status: OptionsState = OptionsState.LOADING,
    val profile: SuggestionProfile? = null,
)

/** Shows what the next suggestion will be based on, so the user can fix it before asking. */
@HiltViewModel
class SuggestionReviewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getOptions: GetTasteSetupOptionsUseCase,
    private val getMyTastes: GetMyTastesUseCase,
    private val getMyDietIds: GetMyDietIdsUseCase,
    private val getTodayNeeds: GetTodayNeedsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionReviewUiState(args = SuggestionArgs.from(savedState)))
    val uiState: StateFlow<SuggestionReviewUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** A reload after an edit keeps the rows on screen; only the first load shows the spinner. */
    fun load() {
        if (loadJob?.isActive == true) return
        if (_uiState.value.profile == null) _uiState.update { it.copy(status = OptionsState.LOADING) }
        loadJob = viewModelScope.launch {
            try {
                val profile = loadProfile()
                _uiState.update { it.copy(status = OptionsState.READY, profile = profile) }
            } catch (e: AppError) {
                _uiState.update { if (it.profile != null) it else it.copy(status = OptionsState.FAILED) }
            }
        }
    }

    private suspend fun loadProfile(): SuggestionProfile = coroutineScope {
        val options = async { getOptions() }
        val tastes = async { getMyTastes() }
        val dietIds = async { getMyDietIds() }
        val needs = async { getTodayNeeds() }
        val tagNames = options.await().tags.associate { it.id to it.name }
        val dietNames = options.await().diets.associate { it.id to it.name }
        SuggestionProfile(
            tastes = tastes.await().sortedByDescending { it.level }.mapNotNull { tagNames[it.tagId] },
            diets = dietIds.await().mapNotNull { dietNames[it] },
            needs = needs.await().sortedBy { it.meal.ordinal },
        )
    }
}
