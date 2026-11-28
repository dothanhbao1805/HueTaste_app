package com.example.huetaste.feature.auth

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.base.common.extension.showKeyboard
import com.base.common.widget.LsTextView
import com.example.huetaste.R
import com.example.huetaste.common.extension.scrollToFocused
import com.example.huetaste.databinding.ActivityForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Resets a forgotten password in three steps: the email, the 6-digit code sent to it, then the new password. */
@AndroidEntryPoint
class ForgotPasswordActivity : LsActivity<ActivityForgotPasswordBinding>(ActivityForgotPasswordBinding::inflate) {
    private val viewModel: ForgotPasswordViewModel by viewModels()

    private var isRestored = false

    private val strengthBars get() = listOf(binding.strengthBar1, binding.strengthBar2, binding.strengthBar3)
    private val codeBoxes
        get() = with(binding) { listOf(codeBox1, codeBox2, codeBox3, codeBox4, codeBox5, codeBox6) }

    override fun onCreate(savedInstanceState: Bundle?) {
        isRestored = savedInstanceState != null
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // A recreated screen keeps what the user typed; only a fresh one starts with the email from the login.
        if (!isRestored) binding.emailInput.setText(viewModel.uiState.value.email)

        binding.backButton.clicks { handleBackPressed() }
        binding.emailInput.trackFocus(binding.emailField)
        binding.passwordInput.trackFocus(binding.passwordField)
        binding.confirmInput.trackFocus(binding.confirmField)
        binding.passwordInput.bindPasswordToggle(binding.passwordToggle)
        binding.confirmInput.bindPasswordToggle(binding.confirmToggle)

        // An error goes away as soon as the user starts fixing the field it belongs to.
        binding.emailInput.doAfterTextChanged { showEmailError(null) }
        binding.passwordInput.doAfterTextChanged { text ->
            showPasswordError(null)
            showPasswordStrength(binding.strengthBlock, strengthBars, binding.strengthText, text?.toString().orEmpty())
        }
        binding.confirmInput.doAfterTextChanged { showConfirmError(null) }
        binding.emailInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) sendCode().let { true } else false
        }
        binding.confirmInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) reset().let { true } else false
        }
        bindCodeInput()

        binding.sendButton.clicks { sendCode() }
        binding.pasteButton.clicks { pasteCode() }
        binding.verifyButton.clicks { verifyCode(submitted = true) }
        binding.resetButton.clicks { reset() }
        binding.resendLink.clicks { viewModel.resendCode() }
        binding.changeEmailLink.clicks { viewModel.goBack() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.resendSeconds.collect(::showResend) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    /** Back walks the steps backwards before it leaves the screen. */
    override fun handleBackPressed() {
        if (viewModel.uiState.value.step == ResetStep.EMAIL) finish() else viewModel.goBack()
    }

    private fun bindCodeInput() {
        val input = binding.codeInput
        input.setOnFocusChangeListener { _, _ -> showCode() }
        input.doAfterTextChanged {
            showCodeError(null)
            showCode()
            verifyCode(submitted = false)
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) verifyCode(submitted = true).let { true } else false
        }
    }

    private fun code() = binding.codeInput.text?.toString().orEmpty()

    /** Spreads the typed digits over the boxes and outlines the box the next digit goes into. */
    private fun showCode() {
        val code = code()
        val focused = binding.codeInput.hasFocus()
        val next = code.length.coerceAtMost(ForgotPasswordViewModel.CODE_LENGTH - 1)
        codeBoxes.forEachIndexed { index, box ->
            box.text = code.getOrNull(index)?.toString().orEmpty()
            box.isActivated = focused && index == next
        }
    }

    private fun sendCode() = viewModel.sendCode(binding.emailInput.text?.toString().orEmpty())

    private fun verifyCode(submitted: Boolean) {
        if (viewModel.uiState.value.step == ResetStep.CODE) viewModel.verifyCode(code(), submitted)
    }

    private fun reset() = viewModel.reset(
        password = binding.passwordInput.text?.toString().orEmpty(),
        confirmation = binding.confirmInput.text?.toString().orEmpty(),
    )

    /** Takes the digits of the copied text, so a whole line copied from the email also works. */
    private fun pasteCode() {
        val clip = getSystemService(ClipboardManager::class.java)?.primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val digits = text.filter(Char::isDigit)
        if (digits.length != ForgotPasswordViewModel.CODE_LENGTH) {
            makeToast(R.string.forgot_paste_empty)
            return
        }
        binding.codeInput.setText(digits)
        binding.codeInput.setSelection(digits.length)
    }

    private fun render(state: ForgotPasswordUiState) {
        binding.emailStep.isVisible = state.step == ResetStep.EMAIL
        binding.codeStep.isVisible = state.step == ResetStep.CODE
        binding.passwordStep.isVisible = state.step == ResetStep.PASSWORD
        binding.sentText.text = getString(R.string.forgot_sent_to, state.email)

        showEmailError(R.string.auth_error_email.takeIf { state.emailInvalid })
        showCodeError(
            when (state.codeError) {
                CodeError.INCOMPLETE -> R.string.forgot_error_code_incomplete
                CodeError.WRONG -> R.string.forgot_error_code_wrong
                CodeError.TOO_MANY -> R.string.forgot_error_code_too_many
                null -> null
            },
        )
        showPasswordError(R.string.auth_error_password.takeIf { state.passwordInvalid })
        showConfirmError(R.string.auth_error_confirm.takeIf { state.confirmMismatch })

        showButton(binding.sendButton, binding.sendSpinner, binding.sendText, state.sending, R.string.forgot_sending, R.string.forgot_send)
        showButton(binding.verifyButton, binding.verifySpinner, binding.verifyText, state.verifying, R.string.forgot_verifying, R.string.forgot_verify)
        showButton(binding.resetButton, binding.resetSpinner, binding.resetText, state.resetting, R.string.forgot_resetting, R.string.forgot_reset)
        binding.verifyButton.isEnabled = !state.isBusy
        binding.changeEmailLink.isEnabled = !state.isBusy
        binding.changeEmailLink.alpha = if (state.isBusy) LOADING_ALPHA else 1f
        showResend(viewModel.resendSeconds.value)
    }

    private fun showResend(seconds: Int) {
        val state = viewModel.uiState.value
        val enabled = seconds == 0 && !state.isBusy
        binding.resendLink.text = when {
            state.sending -> getString(R.string.forgot_sending)
            seconds > 0 -> getString(R.string.forgot_resend_in, seconds)
            else -> getString(R.string.forgot_resend)
        }
        binding.resendLink.isEnabled = enabled
        binding.resendLink.alpha = if (enabled) 1f else LOADING_ALPHA
    }

    private fun showButton(button: View, spinner: View, text: LsTextView, loading: Boolean, @StringRes busy: Int, @StringRes idle: Int) {
        spinner.isVisible = loading
        text.setText(if (loading) busy else idle)
        button.isEnabled = !loading
        button.alpha = if (loading) LOADING_ALPHA else 1f
    }

    private fun handle(event: ForgotPasswordEvent) {
        when (event) {
            ForgotPasswordEvent.CodeSent -> {
                makeToast(R.string.forgot_code_sent)
                // A new code replaces the old one, so the boxes start empty.
                binding.codeInput.text?.clear()
                // The code step may only become visible in the same frame, so focus it once laid out.
                binding.codeInput.post { binding.codeInput.showKeyboard() }
            }
            ForgotPasswordEvent.PasswordReset -> {
                makeToast(R.string.forgot_success)
                finish()
            }
            is ForgotPasswordEvent.Failed -> makeToast(
                when (event.failure) {
                    AuthFailure.NETWORK -> getString(R.string.auth_error_network)
                    else -> event.message?.takeIf { it.isNotBlank() } ?: getString(R.string.auth_error_unknown)
                },
            )
        }
    }

    private fun showEmailError(message: Int?) =
        showFieldError(binding.emailField, binding.emailErrorRow, binding.emailErrorText, message)

    private fun showCodeError(message: Int?) {
        codeBoxes.forEach { it.isSelected = message != null }
        binding.codeErrorRow.isVisible = message != null
        if (message != null) binding.codeErrorText.setText(message)
    }

    private fun showPasswordError(message: Int?) =
        showFieldError(binding.passwordField, binding.passwordErrorRow, binding.passwordErrorText, message)

    private fun showConfirmError(message: Int?) =
        showFieldError(binding.confirmField, binding.confirmErrorRow, binding.confirmErrorText, message)

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard, so leave room for it ourselves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.forgotScroll.updatePadding(bottom = (keyboardHeight - navigationBarHeight).coerceAtLeast(0))
        }
        if (keyboardHeight > 0) binding.forgotScroll.scrollToFocused(currentFocus)
    }

    companion object {
        private const val LOADING_ALPHA = 0.8f

        /** [email] is what the user typed on the login screen, to save typing it again. */
        fun newIntent(context: Context, email: String): Intent =
            Intent(context, ForgotPasswordActivity::class.java).putExtra(ForgotPasswordViewModel.KEY_EMAIL, email.trim())
    }
}
