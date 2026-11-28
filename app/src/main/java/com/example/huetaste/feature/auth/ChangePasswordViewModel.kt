package com.example.huetaste.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.usecase.ChangePasswordUseCase
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

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val oldEmpty: Boolean = false,
    /** The backend rejected the current password. */
    val oldWrong: Boolean = false,
    val passwordInvalid: Boolean = false,
    val confirmMismatch: Boolean = false,
)

sealed interface ChangePasswordEvent {
    object Changed : ChangePasswordEvent
    data class Failed(val failure: AuthFailure, val message: String?) : ChangePasswordEvent
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePassword: ChangePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChangePasswordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    fun submit(oldPassword: String, password: String, confirmation: String) {
        if (job?.isActive == true) return
        val checked = ChangePasswordUiState(
            oldEmpty = oldPassword.isEmpty(),
            passwordInvalid = !AuthValidator.isPasswordValid(password),
            confirmMismatch = !AuthValidator.passwordsMatch(password, confirmation),
        )
        if (checked.oldEmpty || checked.passwordInvalid || checked.confirmMismatch) {
            _uiState.value = checked
            return
        }
        _uiState.value = ChangePasswordUiState(isLoading = true)
        job = viewModelScope.launch {
            try {
                changePassword(oldPassword, password)
                _events.send(ChangePasswordEvent.Changed)
            } catch (e: AppError) {
                val oldRejected = e.code == AppError.WRONG_PASSWORD
                _uiState.update { it.copy(isLoading = false, oldWrong = oldRejected) }
                if (!oldRejected) _events.send(ChangePasswordEvent.Failed(e.toAuthFailure(), e.message))
            }
        }
    }
}
