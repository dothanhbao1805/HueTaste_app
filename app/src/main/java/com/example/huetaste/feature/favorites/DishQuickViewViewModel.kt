package com.example.huetaste.feature.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DishDetail
import com.example.huetaste.domain.usecase.GetDishDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DishQuickViewState {
    object Loading : DishQuickViewState
    data class Error(val error: AppError) : DishQuickViewState
    data class Content(val detail: DishDetail) : DishQuickViewState
}

/** Loads `GET /mon-an/{id}` for the quick view sheet; the dish id arrives as a fragment argument. */
@HiltViewModel
class DishQuickViewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getDishDetail: GetDishDetailUseCase,
) : ViewModel() {
    val dishId: String = checkNotNull(savedState[DishQuickViewSheet.ARG_DISH_ID])

    private val _state = MutableStateFlow<DishQuickViewState>(DishQuickViewState.Loading)
    val state: StateFlow<DishQuickViewState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    fun load() {
        job?.cancel()
        _state.value = DishQuickViewState.Loading
        job = viewModelScope.launch {
            _state.value = try {
                DishQuickViewState.Content(getDishDetail(dishId))
            } catch (e: AppError) {
                DishQuickViewState.Error(e)
            }
        }
    }
}
