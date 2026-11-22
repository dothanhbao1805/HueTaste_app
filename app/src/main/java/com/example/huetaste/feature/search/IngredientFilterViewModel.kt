package com.example.huetaste.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.usecase.SearchIngredientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientFilterUiState(
    /** Ingredient names in the order they were picked; the backend filters by name. */
    val selected: List<String> = emptyList(),
    val results: List<Ingredient> = emptyList(),
    val isSearching: Boolean = true,
    val failed: Boolean = false,
)

/** The ingredient sheet: `GET /nguyen-lieu` as the user types, and the names picked so far. */
@OptIn(FlowPreview::class)
@HiltViewModel
class IngredientFilterViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val searchIngredients: SearchIngredientsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IngredientFilterUiState(
            selected = savedState.get<ArrayList<String>>(IngredientFilterSheet.ARG_SELECTED).orEmpty(),
        ),
    )
    val uiState: StateFlow<IngredientFilterUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            query.debounce(SEARCH_DEBOUNCE_MS).collectLatest { text ->
                _uiState.update { it.copy(isSearching = true, failed = false) }
                try {
                    val results = searchIngredients(text)
                    _uiState.update { it.copy(results = results, isSearching = false) }
                } catch (e: AppError) {
                    _uiState.update { it.copy(results = emptyList(), isSearching = false, failed = true) }
                }
            }
        }
    }

    fun search(text: String) {
        query.value = text
    }

    fun toggle(name: String) {
        val selected = _uiState.value.selected
        save(if (name in selected) selected - name else selected + name)
    }

    fun reset() = save(emptyList())

    private fun save(selected: List<String>) {
        savedState[IngredientFilterSheet.ARG_SELECTED] = ArrayList(selected)
        _uiState.update { it.copy(selected = selected) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
