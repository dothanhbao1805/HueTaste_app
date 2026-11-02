package com.example.huetaste.feature.auth

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeLinks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.startMainActivity
import com.example.huetaste.databinding.ActivityRegisterBinding
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.model.PasswordStrength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : LsActivity<ActivityRegisterBinding>(ActivityRegisterBinding::inflate) {
    private val viewModel: RegisterViewModel by viewModels()

    private var termsAccepted = false

    private val strengthBars get() = listOf(binding.strengthBar1, binding.strengthBar2, binding.strengthBar3)

    override fun onCreate(savedInstanceState: Bundle?) {
        termsAccepted = savedInstanceState?.getBoolean(KEY_TERMS) ?: false
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }

        binding.nameInput.trackFocus(binding.nameField)
        binding.emailInput.trackFocus(binding.emailField)
        binding.passwordInput.trackFocus(binding.passwordField)
        binding.confirmInput.trackFocus(binding.confirmField)
        binding.passwordInput.bindPasswordToggle(binding.passwordToggle)
        binding.confirmInput.bindPasswordToggle(binding.confirmToggle)

        // An error goes away as soon as the user starts fixing the field it belongs to.
        binding.nameInput.doAfterTextChanged { showNameError(null) }
        binding.emailInput.doAfterTextChanged { showEmailError(null) }
        binding.passwordInput.doAfterTextChanged { text ->
            showPasswordError(null)
            showStrength(text?.toString().orEmpty())
        }
        binding.confirmInput.doAfterTextChanged { showConfirmError(null) }
        binding.confirmInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit().let { true } else false
        }

        showTerms()
        binding.termsBox.clicks {
            termsAccepted = !termsAccepted
            binding.termsCheckbox.isSelected = termsAccepted
        }

        binding.submitButton.clicks { submit() }
        binding.loginLink.clicks { handleBackPressed() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    private fun submit() {
        if (!termsAccepted) makeToast(R.string.auth_error_terms)
        viewModel.submit(
            name = binding.nameInput.text?.toString().orEmpty(),
            email = binding.emailInput.text?.toString().orEmpty(),
            password = binding.passwordInput.text?.toString().orEmpty(),
            confirmation = binding.confirmInput.text?.toString().orEmpty(),
            termsAccepted = termsAccepted,
        )
    }

    /** The terms and privacy policy have no page yet, so their links only say they are coming. */
    private fun showTerms() {
        binding.termsCheckbox.isSelected = termsAccepted
        val terms = getString(R.string.register_terms_link)
        val privacy = getString(R.string.register_privacy_link)
        binding.termsText.text = getString(R.string.register_terms, terms, privacy)
        val comingSoon = View.OnClickListener { makeToast(R.string.auth_coming_soon) }
        binding.termsText.makeLinks(true, ContextCompat.getColor(this, R.color.hue_primary), terms to comingSoon, privacy to comingSoon)
    }

    private fun render(state: RegisterUiState) {
        showNameError(R.string.auth_error_name.takeIf { state.nameInvalid })
        showEmailError(
            when {
                state.emailInvalid -> R.string.auth_error_email
                state.emailTaken -> R.string.auth_error_email_taken
                else -> null
            },
        )
        showPasswordError(R.string.auth_error_password.takeIf { state.passwordInvalid })
        showConfirmError(R.string.auth_error_confirm.takeIf { state.confirmMismatch })

        binding.submitSpinner.isVisible = state.isLoading
        binding.submitText.setText(if (state.isLoading) R.string.register_submitting else R.string.register_submit)
        binding.submitButton.isEnabled = !state.isLoading
        binding.submitButton.alpha = if (state.isLoading) LOADING_ALPHA else 1f
    }

    private fun handle(event: AuthEvent) {
        when (event) {
            AuthEvent.Authenticated -> {
                startMainActivity()
                finish()
            }
            is AuthEvent.Failed -> makeToast(
                when (event.failure) {
                    AuthFailure.NETWORK -> getString(R.string.auth_error_network)
                    else -> event.message?.takeIf { it.isNotBlank() } ?: getString(R.string.auth_error_unknown)
                },
            )
        }
    }

    private fun showStrength(password: String) {
        binding.strengthBlock.isVisible = password.isNotEmpty()
        if (password.isEmpty()) return
        val strength = AuthValidator.strengthOf(password)
        val level = when (strength) {
            PasswordStrength.WEAK -> 1
            PasswordStrength.MEDIUM -> 2
            PasswordStrength.STRONG -> 3
        }
        val color = when (strength) {
            PasswordStrength.WEAK -> R.color.hue_error
            PasswordStrength.MEDIUM -> R.color.hue_secondary_container
            PasswordStrength.STRONG -> R.color.hue_primary
        }
        strengthBars.forEachIndexed { index, bar ->
            bar.backgroundTintList = colorList(if (index < level) color else R.color.hue_surface_container_high)
        }
        val label = when (strength) {
            PasswordStrength.WEAK -> R.string.register_strength_weak
            PasswordStrength.MEDIUM -> R.string.register_strength_medium
            PasswordStrength.STRONG -> R.string.register_strength_strong
        }
        binding.strengthText.text = getString(R.string.register_strength, getString(label))
    }

    private fun colorList(@ColorRes color: Int) = ColorStateList.valueOf(ContextCompat.getColor(this, color))

    private fun showNameError(message: Int?) =
        showFieldError(binding.nameField, binding.nameErrorRow, binding.nameErrorText, message)

    private fun showEmailError(message: Int?) =
        showFieldError(binding.emailField, binding.emailErrorRow, binding.emailErrorText, message)

    private fun showPasswordError(message: Int?) =
        showFieldError(binding.passwordField, binding.passwordErrorRow, binding.passwordErrorText, message)

    private fun showConfirmError(message: Int?) =
        showFieldError(binding.confirmField, binding.confirmErrorRow, binding.confirmErrorText, message)

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard, so leave room for it ourselves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.registerScroll.updatePadding(bottom = (keyboardHeight - navigationBarHeight).coerceAtLeast(0))
        }
        if (keyboardHeight > 0) binding.registerScroll.scrollToFocused(currentFocus)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_TERMS, termsAccepted)
    }

    companion object {
        private const val KEY_TERMS = "register_terms_accepted"
        private const val LOADING_ALPHA = 0.8f

        fun newIntent(context: Context) = android.content.Intent(context, RegisterActivity::class.java)
    }
}
