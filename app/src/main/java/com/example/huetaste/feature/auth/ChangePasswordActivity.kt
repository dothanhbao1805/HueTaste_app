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
import com.example.huetaste.common.extension.scrollToFocused
import com.example.huetaste.databinding.ActivityChangePasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Lets the signed-in user replace their password by confirming the current one. */
@AndroidEntryPoint
class ChangePasswordActivity : LsActivity<ActivityChangePasswordBinding>(ActivityChangePasswordBinding::inflate) {
    private val viewModel: ChangePasswordViewModel by viewModels()

    private val strengthBars get() = listOf(binding.strengthBar1, binding.strengthBar2, binding.strengthBar3)

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }

        binding.oldInput.trackFocus(binding.oldField)
        binding.passwordInput.trackFocus(binding.passwordField)
        binding.confirmInput.trackFocus(binding.confirmField)
        binding.oldInput.bindPasswordToggle(binding.oldToggle)
        binding.passwordInput.bindPasswordToggle(binding.passwordToggle)
        binding.confirmInput.bindPasswordToggle(binding.confirmToggle)

        // An error goes away as soon as the user starts fixing the field it belongs to.
        binding.oldInput.doAfterTextChanged { showOldError(null) }
        binding.passwordInput.doAfterTextChanged { text ->
            showPasswordError(null)
            showPasswordStrength(binding.strengthBlock, strengthBars, binding.strengthText, text?.toString().orEmpty())
        }
        binding.confirmInput.doAfterTextChanged { showConfirmError(null) }
        binding.confirmInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit().let { true } else false
        }

        binding.submitButton.clicks { submit() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    private fun submit() = viewModel.submit(
        oldPassword = binding.oldInput.text?.toString().orEmpty(),
        password = binding.passwordInput.text?.toString().orEmpty(),
        confirmation = binding.confirmInput.text?.toString().orEmpty(),
    )

    private fun render(state: ChangePasswordUiState) {
        showOldError(
            when {
                state.oldEmpty -> R.string.change_error_old_empty
                state.oldWrong -> R.string.change_error_old_wrong
                else -> null
            },
        )
        showPasswordError(R.string.auth_error_password.takeIf { state.passwordInvalid })
        showConfirmError(R.string.auth_error_confirm.takeIf { state.confirmMismatch })

        binding.submitSpinner.isVisible = state.isLoading
        binding.submitText.setText(if (state.isLoading) R.string.change_submitting else R.string.profile_password_title)
        binding.submitButton.isEnabled = !state.isLoading
        binding.submitButton.alpha = if (state.isLoading) LOADING_ALPHA else 1f
    }

    private fun handle(event: ChangePasswordEvent) {
        when (event) {
            ChangePasswordEvent.Changed -> {
                makeToast(R.string.change_success)
                finish()
            }
            is ChangePasswordEvent.Failed -> makeToast(
                when (event.failure) {
                    AuthFailure.NETWORK -> getString(R.string.auth_error_network)
                    else -> event.message?.takeIf { it.isNotBlank() } ?: getString(R.string.auth_error_unknown)
                },
            )
        }
    }

    private fun showOldError(message: Int?) =
        showFieldError(binding.oldField, binding.oldErrorRow, binding.oldErrorText, message)

    private fun showPasswordError(message: Int?) =
        showFieldError(binding.passwordField, binding.passwordErrorRow, binding.passwordErrorText, message)

    private fun showConfirmError(message: Int?) =
        showFieldError(binding.confirmField, binding.confirmErrorRow, binding.confirmErrorText, message)

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard, so leave room for it ourselves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.changeScroll.updatePadding(bottom = (keyboardHeight - navigationBarHeight).coerceAtLeast(0))
        }
        if (keyboardHeight > 0) binding.changeScroll.scrollToFocused(currentFocus)
    }

    companion object {
        private const val LOADING_ALPHA = 0.8f

        fun newIntent(context: Context): Intent = Intent(context, ChangePasswordActivity::class.java)
    }
}
