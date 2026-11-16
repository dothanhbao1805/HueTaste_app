package com.example.huetaste.feature.diet_preferences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.usecase.GetDietOptionsUseCase
import com.example.huetaste.domain.usecase.GetMyDietIdsUseCase
import com.example.huetaste.domain.usecase.SaveDietsUseCase
import com.example.huetaste.feature.taste_setup.OptionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietPreferencesUiState(
    val optionsState: OptionsState = OptionsState.LOADING,
    val diets: List<DietOption> = emptyList(),
    /** Empty means "Ăn bình thường": the backend has no such diet. */
    val selectedDietIds: Set<String> = emptySet(),
    /** What the backend holds now; the screen is dirty when the choices differ from it. */
    val savedDietIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
) {
    val isDirty get() = selectedDietIds != savedDietIds
}

sealed interface DietPreferencesEvent {
    data object Saved : DietPreferencesEvent
    data class SaveFailed(val network: Boolean) : DietPreferencesEvent
}

/** Edits the signed-in user's diets (`PUT /users/me/che-do-an` replaces them all). */
@HiltViewModel
class DietPreferencesViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getDietOptions: GetDietOptionsUseCase,
    private val getMyDietIds: GetMyDietIdsUseCase,
    private val saveDiets: SaveDietsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DietPreferencesUiState())
    val uiState: StateFlow<DietPreferencesUiState> = _uiState.asStateFlow()

    private val _events = Channel<DietPreferencesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(optionsState = OptionsState.LOADING) }
        viewModelScope.launch {
            try {
                val (diets, mine) = coroutineScope {
                    val diets = async { getDietOptions() }
                    val mine = async { getMyDietIds() }
                    diets.await() to mine.await()
                }
                val catalogIds = diets.map { it.id }.toSet()
                val saved = mine.filter { it in catalogIds }.toSet()
                // Choices made before the process died win over what the backend holds.
                val selected = savedState.get<ArrayList<String>>(KEY_DIETS)?.toSet()?.intersect(catalogIds) ?: saved
                _uiState.update {
                    it.copy(optionsState = OptionsState.READY, diets = diets, selectedDietIds = selected, savedDietIds = saved)
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(optionsState = OptionsState.FAILED) }
            }
        }
    }

    fun selectNormal() = edit { it.copy(selectedDietIds = emptySet()) }

    /** Unselecting the last diet leaves the set empty, which is "Ăn bình thường" again. */
    fun toggleDiet(id: String) = edit {
        it.copy(selectedDietIds = if (id in it.selectedDietIds) it.selectedDietIds - id else it.selectedDietIds + id)
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || !state.isDirty) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                saveDiets(state.selectedDietIds.toList())
                _uiState.update { it.copy(isSaving = false, savedDietIds = state.selectedDietIds) }
                _events.send(DietPreferencesEvent.Saved)
            } catch (e: AppError) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(DietPreferencesEvent.SaveFailed(network = e.code == AppError.NETWORK))
            }
        }
    }

    private fun edit(transform: (DietPreferencesUiState) -> DietPreferencesUiState) {
        val state = _uiState.updateAndGet(transform)
        savedState[KEY_DIETS] = ArrayList(state.selectedDietIds)
    }

    private companion object {
        const val KEY_DIETS = "diet_prefs_ids"
    }
}
