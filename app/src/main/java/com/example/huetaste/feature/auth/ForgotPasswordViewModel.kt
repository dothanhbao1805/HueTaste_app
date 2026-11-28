package com.example.huetaste.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.usecase.RequestPasswordResetUseCase
import com.example.huetaste.domain.usecase.ResetPasswordUseCase
import com.example.huetaste.domain.usecase.VerifyResetCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The user asks for a code by email, enters the 6-digit code, then picks the new password. */
enum class ResetStep { EMAIL, CODE, PASSWORD }

enum class CodeError { INCOMPLETE, WRONG, TOO_MANY }

data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.EMAIL,
    /** The address the code was sent to. */
    val email: String = "",
    /** A code is being requested, the first time or again from the code step. */
    val sending: Boolean = false,
    val verifying: Boolean = false,
    val resetting: Boolean = false,
    val emailInvalid: Boolean = false,
    val codeError: CodeError? = null,
    /** The code the backend last rejected, so the same digits are not sent again (each try counts). */
    val rejectedCode: String? = null,
    val passwordInvalid: Boolean = false,
    val confirmMismatch: Boolean = false,
) {
    val isBusy get() = sending || verifying || resetting
}

sealed interface ForgotPasswordEvent {
    object CodeSent : ForgotPasswordEvent
    object PasswordReset : ForgotPasswordEvent
    data class Failed(val failure: AuthFailure, val message: String?) : ForgotPasswordEvent
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestPasswordReset: RequestPasswordResetUseCase,
    private val verifyResetCode: VerifyResetCodeUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    // The step, address and verified code survive process death; the email first comes from the login screen's
    // extra of the same key.
    private val _uiState = MutableStateFlow(
        ForgotPasswordUiState(
            step = savedState.get<String>(KEY_STEP)?.let(ResetStep::valueOf) ?: ResetStep.EMAIL,
            email = savedState.get<String>(KEY_EMAIL).orEmpty(),
        ),
    )
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /** Seconds until another code may be sent; kept apart so the ticking does not redraw the form. */
    private val _resendSeconds = MutableStateFlow(0)
    val resendSeconds: StateFlow<Int> = _resendSeconds.asStateFlow()

    private val _events = Channel<ForgotPasswordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null
    private var countdown: Job? = null

    init {
        startCountdown()
    }

    fun sendCode(email: String) {
        if (job?.isActive == true) return
        if (!AuthValidator.isEmailValid(email)) {
            _uiState.update { it.copy(emailInvalid = true) }
            return
        }
        val address = email.trim()
        _uiState.update { it.copy(sending = true, emailInvalid = false) }
        job = viewModelScope.launch {
            try {
                requestPasswordReset(address)
                savedState[KEY_EMAIL] = address
                moveTo(ResetStep.CODE)
                savedState[KEY_RESEND_AT] = System.currentTimeMillis() + RESEND_DELAY_MS
                startCountdown()
                _uiState.value = ForgotPasswordUiState(step = ResetStep.CODE, email = address)
                _events.send(ForgotPasswordEvent.CodeSent)
            } catch (e: AppError) {
                _uiState.update { it.copy(sending = false) }
                _events.send(ForgotPasswordEvent.Failed(e.toAuthFailure(), e.message))
            }
        }
    }

    /** Sends a new code to the same address once the wait is over; the backend drops the previous code. */
    fun resendCode() {
        if (_resendSeconds.value == 0) sendCode(_uiState.value.email)
    }

    /** Called with every change of the code input; a full code is checked straight away. */
    fun verifyCode(code: String, submitted: Boolean) {
        if (job?.isActive == true) return
        val state = _uiState.value
        if (code.length < CODE_LENGTH) {
            if (submitted) _uiState.update { it.copy(codeError = CodeError.INCOMPLETE) }
            return
        }
        // A recreated screen restores the same digits; trying them again would only use up an attempt.
        if (code == state.rejectedCode) return
        _uiState.update { it.copy(verifying = true, codeError = null) }
        job = viewModelScope.launch {
            try {
                verifyResetCode(state.email, code)
                savedState[KEY_CODE] = code
                moveTo(ResetStep.PASSWORD)
                _uiState.update { it.copy(step = ResetStep.PASSWORD, verifying = false) }
            } catch (e: AppError) {
                _uiState.update { it.copy(verifying = false) }
                onCodeFailure(e, code)
            }
        }
    }

    fun reset(password: String, confirmation: String) {
        if (job?.isActive == true) return
        val checked = _uiState.value.copy(
            passwordInvalid = !AuthValidator.isPasswordValid(password),
            confirmMismatch = !AuthValidator.passwordsMatch(password, confirmation),
        )
        if (checked.passwordInvalid || checked.confirmMismatch) {
            _uiState.value = checked
            return
        }
        val code = savedState.get<String>(KEY_CODE).orEmpty()
        _uiState.value = checked.copy(resetting = true)
        job = viewModelScope.launch {
            try {
                resetPassword(checked.email, code, password)
                _events.send(ForgotPasswordEvent.PasswordReset)
            } catch (e: AppError) {
                _uiState.update { it.copy(resetting = false) }
                // The code may have expired while the password was typed: back to the code step to get a new one.
                if (isCodeRejection(e)) {
                    moveTo(ResetStep.CODE)
                    _uiState.update { it.copy(step = ResetStep.CODE) }
                }
                onCodeFailure(e, code)
            }
        }
    }

    /** Back one step: from the password to the code, from the code to the email. */
    fun goBack() {
        job?.cancel()
        val previous = if (_uiState.value.step == ResetStep.PASSWORD) ResetStep.CODE else ResetStep.EMAIL
        moveTo(previous)
        _uiState.value = ForgotPasswordUiState(step = previous, email = _uiState.value.email)
    }

    private suspend fun onCodeFailure(e: AppError, code: String) {
        val error = when (e.code) {
            AppError.OTP_TOO_MANY -> CodeError.TOO_MANY
            AppError.INVALID_RESET_TOKEN -> CodeError.WRONG
            else -> null
        }
        if (error == null) {
            _events.send(ForgotPasswordEvent.Failed(e.toAuthFailure(), e.message))
            return
        }
        _uiState.update { it.copy(codeError = error, rejectedCode = code) }
    }

    private fun isCodeRejection(e: AppError) = e.code == AppError.OTP_TOO_MANY || e.code == AppError.INVALID_RESET_TOKEN

    private fun moveTo(step: ResetStep) {
        savedState[KEY_STEP] = step.name
    }

    private fun startCountdown() {
        countdown?.cancel()
        val resendAt = savedState.get<Long>(KEY_RESEND_AT) ?: return
        countdown = viewModelScope.launch {
            while (true) {
                val left = ((resendAt - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0).toInt()
                _resendSeconds.value = left
                if (left == 0) break
                delay(1000)
            }
        }
    }

    companion object {
        const val KEY_EMAIL = "forgot_email"
        const val CODE_LENGTH = 6
        private const val KEY_STEP = "forgot_step"
        private const val KEY_CODE = "forgot_code"
        private const val KEY_RESEND_AT = "forgot_resend_at"

        /** Matches the backend's wait before it sends another code. */
        private const val RESEND_DELAY_MS = 60_000L
    }
}
