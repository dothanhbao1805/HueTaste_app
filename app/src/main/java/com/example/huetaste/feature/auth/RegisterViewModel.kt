package com.example.huetaste.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val nameInvalid: Boolean = false,
    val emailInvalid: Boolean = false,
    val emailTaken: Boolean = false,
    val passwordInvalid: Boolean = false,
    val confirmMismatch: Boolean = false,
) {
    val hasFieldErrors get() = nameInvalid || emailInvalid || emailTaken || passwordInvalid || confirmMismatch
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    fun submit(name: String, email: String, password: String, confirmation: String, termsAccepted: Boolean) {
        if (job?.isActive == true) return
        val checked = RegisterUiState(
            nameInvalid = !AuthValidator.isNameValid(name),
            emailInvalid = !AuthValidator.isEmailValid(email),
            passwordInvalid = !AuthValidator.isPasswordValid(password),
            confirmMismatch = !AuthValidator.passwordsMatch(password, confirmation),
        )
        if (checked.hasFieldErrors || !termsAccepted) {
            _uiState.value = checked
            return
        }
        _uiState.value = RegisterUiState(isLoading = true)
        job = viewModelScope.launch {
            try {
                register(name, email, password)
                _events.send(AuthEvent.Authenticated(needsTasteSetup = true))
            } catch (e: AppError) {
                val failure = e.toAuthFailure()
                _uiState.value = RegisterUiState(emailTaken = failure == AuthFailure.EMAIL_TAKEN)
                if (failure != AuthFailure.EMAIL_TAKEN) _events.send(AuthEvent.Failed(failure, e.message))
            }
        }
    }
}
