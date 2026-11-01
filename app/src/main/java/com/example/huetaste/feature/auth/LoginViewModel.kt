package com.example.huetaste.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.usecase.ContinueAsGuestUseCase
import com.example.huetaste.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val emailInvalid: Boolean = false,
    val passwordEmpty: Boolean = false,
    val failure: AuthFailure? = null,
    /** The backend's own message, shown for failures the screen has no wording for (a blocked account). */
    val failureMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val continueAsGuest: ContinueAsGuestUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    fun submit(email: String, password: String) {
        if (job?.isActive == true) return
        val emailInvalid = !AuthValidator.isEmailValid(email)
        val passwordEmpty = password.isEmpty()
        if (emailInvalid || passwordEmpty) {
            _uiState.value = LoginUiState(emailInvalid = emailInvalid, passwordEmpty = passwordEmpty)
            return
        }
        _uiState.value = LoginUiState(isLoading = true)
        job = viewModelScope.launch {
            try {
                login(email, password)
                _events.send(AuthEvent.Authenticated)
            } catch (e: AppError) {
                _uiState.value = LoginUiState(failure = e.toAuthFailure(), failureMessage = e.message)
            }
        }
    }

    /** Uses the app without an account; the choice is remembered so the next launch goes straight to the main screen. */
    fun skipLogin() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            continueAsGuest()
            _events.send(AuthEvent.Authenticated)
        }
    }
}
