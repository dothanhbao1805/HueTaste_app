package com.example.huetaste.feature.taste_preferences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.TastePreference
import com.example.huetaste.domain.model.TasteTag
import com.example.huetaste.domain.model.TasteTagType
import com.example.huetaste.domain.usecase.GetMyTastesUseCase
import com.example.huetaste.domain.usecase.GetTasteSetupOptionsUseCase
import com.example.huetaste.domain.usecase.SaveTastesUseCase
import com.example.huetaste.feature.taste_setup.OptionsState
import com.example.huetaste.feature.taste_setup.SpiceLevel
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

data class TastePreferencesUiState(
    val optionsState: OptionsState = OptionsState.LOADING,
    val flavorTags: List<TasteTag> = emptyList(),
    val styleTags: List<TasteTag> = emptyList(),
    /** The backend "Cay" flavour, shown as the spiciness control instead of a chip; null when the catalog lacks it. */
    val spicyTag: TasteTag? = null,
    val spiceLevel: SpiceLevel = SpiceLevel.NONE,
    val selectedTagIds: Set<String> = emptySet(),
    /** What the backend holds now; the screen is dirty when the choices differ from it. */
    val savedSpiceLevel: SpiceLevel = SpiceLevel.NONE,
    val savedTagIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
) {
    val isDirty get() = spiceLevel != savedSpiceLevel || selectedTagIds != savedTagIds
    val selectedFlavors get() = flavorTags.filter { it.id in selectedTagIds }
    val selectedStyles get() = styleTags.filter { it.id in selectedTagIds }
}

sealed interface TastePreferencesEvent {
    data object Saved : TastePreferencesEvent
    data class SaveFailed(val network: Boolean) : TastePreferencesEvent
}

/** Edits the signed-in user's spiciness, liked flavours and dish styles (`PUT /users/me/so-thich` replaces them all). */
@HiltViewModel
class TastePreferencesViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getOptions: GetTasteSetupOptionsUseCase,
    private val getMyTastes: GetMyTastesUseCase,
    private val saveTastes: SaveTastesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TastePreferencesUiState())
    val uiState: StateFlow<TastePreferencesUiState> = _uiState.asStateFlow()

    private val _events = Channel<TastePreferencesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(optionsState = OptionsState.LOADING) }
        viewModelScope.launch {
            try {
                val (options, mine) = coroutineScope {
                    val options = async { getOptions() }
                    val mine = async { getMyTastes() }
                    options.await() to mine.await()
                }
                val flavors = options.tags.filter { it.type == TasteTagType.FLAVOR }
                val spicy = flavors.firstOrNull { it.name.equals(SPICY_TAG_NAME, ignoreCase = true) }
                val styles = options.tags.filter { it.type == TasteTagType.STYLE }
                val catalogIds = (flavors + styles).map { it.id }.toSet() - setOfNotNull(spicy?.id)
                val savedTags = mine.map { it.tagId }.filter { it in catalogIds }.toSet()
                val savedSpice = spicy?.let { tag -> mine.firstOrNull { it.tagId == tag.id } }?.let { spiceLevelOf(it.level) }
                    ?: SpiceLevel.NONE
                // Choices made before the process died win over what the backend holds.
                val tags = savedState.get<ArrayList<String>>(KEY_TAGS)?.toSet()?.intersect(catalogIds) ?: savedTags
                val spice = savedState.get<String>(KEY_SPICE)?.let(SpiceLevel::valueOf) ?: savedSpice
                _uiState.update {
                    it.copy(
                        optionsState = OptionsState.READY,
                        flavorTags = flavors - setOfNotNull(spicy),
                        styleTags = styles,
                        spicyTag = spicy,
                        spiceLevel = spice,
                        selectedTagIds = tags,
                        savedSpiceLevel = savedSpice,
                        savedTagIds = savedTags,
                    )
                }
            } catch (e: AppError) {
                _uiState.update { it.copy(optionsState = OptionsState.FAILED) }
            }
        }
    }

    fun selectSpice(level: SpiceLevel) = edit { it.copy(spiceLevel = level) }

    fun toggleTag(id: String) = edit {
        it.copy(selectedTagIds = if (id in it.selectedTagIds) it.selectedTagIds - id else it.selectedTagIds + id)
    }

    /** Back to what is saved. */
    fun reset() = edit { it.copy(spiceLevel = it.savedSpiceLevel, selectedTagIds = it.savedTagIds) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || !state.isDirty) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                saveTastes(state.toTastes())
                _uiState.update { it.copy(isSaving = false, savedSpiceLevel = state.spiceLevel, savedTagIds = state.selectedTagIds) }
                _events.send(TastePreferencesEvent.Saved)
            } catch (e: AppError) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(TastePreferencesEvent.SaveFailed(network = e.code == AppError.NETWORK))
            }
        }
    }

    private fun TastePreferencesUiState.toTastes(): List<TastePreference> {
        val liked = selectedTagIds.map { TastePreference(it, LIKED_LEVEL) }
        val spice = spicyTag?.let { tag -> spiceLevel.level?.let { TastePreference(tag.id, it) } }
        return liked + listOfNotNull(spice)
    }

    private fun edit(transform: (TastePreferencesUiState) -> TastePreferencesUiState) {
        val state = _uiState.updateAndGet(transform)
        savedState[KEY_SPICE] = state.spiceLevel.name
        savedState[KEY_TAGS] = ArrayList(state.selectedTagIds)
    }

    /** The backend stores 1 to 5; the control has four steps, so levels in between round to the nearest step. */
    private fun spiceLevelOf(level: Int) = when {
        level <= 0 -> SpiceLevel.NONE
        level <= 2 -> SpiceLevel.MILD
        level == 3 -> SpiceLevel.MEDIUM
        else -> SpiceLevel.HUE
    }

    private companion object {
        const val SPICY_TAG_NAME = "Cay"
        const val LIKED_LEVEL = 4
        const val KEY_SPICE = "taste_prefs_spice"
        const val KEY_TAGS = "taste_prefs_tags"
    }
}
