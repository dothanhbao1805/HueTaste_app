package com.example.huetaste.feature.auth

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
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
import com.example.huetaste.R
import com.example.huetaste.databinding.ActivityLoginBinding
import com.example.huetaste.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : LsActivity<ActivityLoginBinding>(ActivityLoginBinding::inflate) {
    private val viewModel: LoginViewModel by viewModels()

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }

        binding.emailInput.trackFocus(binding.emailField)
        binding.passwordInput.trackFocus(binding.passwordField)
        binding.passwordInput.bindPasswordToggle(binding.passwordToggle)

        // An error goes away as soon as the user starts fixing the field it belongs to.
        binding.emailInput.doAfterTextChanged { showEmailError(null) }
        binding.passwordInput.doAfterTextChanged {
            showPasswordError(null)
            binding.loginErrorBanner.isVisible = false
        }
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit().let { true } else false
        }

        binding.submitButton.clicks { submit() }
        binding.guestButton.clicks { viewModel.skipLogin() }
        binding.registerLink.clicks { startActivity(RegisterActivity.newIntent(this)) }
        binding.forgotButton.clicks { makeToast(R.string.auth_coming_soon) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    private fun submit() {
        viewModel.submit(binding.emailInput.text?.toString().orEmpty(), binding.passwordInput.text?.toString().orEmpty())
    }

    private fun render(state: LoginUiState) {
        showEmailError(R.string.auth_error_email.takeIf { state.emailInvalid })
        showPasswordError(R.string.auth_error_password_empty.takeIf { state.passwordEmpty })

        val banner = when (state.failure) {
            null -> null
            AuthFailure.INVALID_CREDENTIALS, AuthFailure.EMAIL_TAKEN -> getString(R.string.auth_error_credentials)
            AuthFailure.NETWORK -> getString(R.string.auth_error_network)
            AuthFailure.UNKNOWN -> state.failureMessage?.takeIf { it.isNotBlank() } ?: getString(R.string.auth_error_unknown)
        }
        binding.loginErrorBanner.isVisible = banner != null
        binding.loginErrorText.text = banner

        binding.submitSpinner.isVisible = state.isLoading
        binding.submitText.setText(if (state.isLoading) R.string.login_submitting else R.string.login_submit)
        listOf(binding.submitButton, binding.guestButton).forEach { button ->
            button.isEnabled = !state.isLoading
            button.alpha = if (state.isLoading) LOADING_ALPHA else 1f
        }
    }

    private fun handle(event: AuthEvent) {
        if (event is AuthEvent.Authenticated) {
            startActivity(MainActivity.newRootIntent(this))
            finish()
        }
    }

    private fun showEmailError(message: Int?) =
        showFieldError(binding.emailField, binding.emailErrorRow, binding.emailErrorText, message)

    private fun showPasswordError(message: Int?) =
        showFieldError(binding.passwordField, binding.passwordErrorRow, binding.passwordErrorText, message)

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard, so leave room for it ourselves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.loginScroll.updatePadding(bottom = (keyboardHeight - navigationBarHeight).coerceAtLeast(0))
        }
        if (keyboardHeight > 0) binding.loginScroll.scrollToFocused(currentFocus)
    }

    companion object {
        private const val LOADING_ALPHA = 0.8f

        /** Opens the login as the only screen, for a launch without a session or after the session ended. */
        fun newRootIntent(context: Context): Intent = Intent(context, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
