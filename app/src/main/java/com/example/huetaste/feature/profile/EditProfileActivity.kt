package com.example.huetaste.feature.profile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.scrollIntoViewAboveKeyboard
import com.example.huetaste.common.extension.scrollToFocused
import com.example.huetaste.common.ui.DiscardChangesDialog
import com.example.huetaste.databinding.ActivityEditProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Lets the signed-in user change their display name and profile photo; the email is fixed. */
@AndroidEntryPoint
class EditProfileActivity : LsActivity<ActivityEditProfileBinding>(ActivityEditProfileBinding::inflate) {
    private val viewModel: EditProfileViewModel by viewModels()

    /** The picked file or avatar URL on screen, so typing does not reload the photo. */
    private var shownPhoto: Any? = null

    // viewModel is read in the callback only: the launcher is registered before the Activity is ready for it.
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickAvatar(uri)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }
        binding.avatarButton.clicks { pickPhoto() }
        binding.changePhotoButton.clicks { pickPhoto() }
        binding.saveButton.clicks { viewModel.save() }

        binding.nameInput.setOnFocusChangeListener { field, hasFocus ->
            binding.nameField.isActivated = hasFocus
            if (hasFocus) field.scrollIntoViewAboveKeyboard()
        }
        binding.nameInput.doAfterTextChanged { viewModel.setName(it?.toString().orEmpty()) }
        binding.nameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) viewModel.save().let { true } else false
        }

        supportFragmentManager.setFragmentResultListener(DiscardChangesDialog.REQUEST_KEY, this) { _, _ -> finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    override fun handleBackPressed() {
        if (viewModel.uiState.value.isDirty) DiscardChangesDialog.show(supportFragmentManager) else finish()
    }

    private fun pickPhoto() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun render(state: EditProfileUiState) = with(binding) {
        val user = state.user ?: return@with
        // Only an outside change (the first load) rewrites the box; typing already matches the state.
        if (nameInput.text?.toString() != state.name) {
            nameInput.setText(state.name)
            nameInput.setSelection(nameInput.text?.length ?: 0)
        }
        emailText.text = user.email
        showPhoto(state)

        nameField.isSelected = state.nameInvalid
        nameErrorRow.isVisible = state.nameInvalid

        imageSpinner.isVisible = state.isPreparingImage
        listOf(avatarButton, changePhotoButton).forEach { it.isEnabled = !state.isBusy }
        saveSpinner.isVisible = state.isSaving
        saveText.setText(if (state.isSaving) R.string.edit_profile_saving else R.string.edit_profile_save)
        saveButton.isEnabled = state.isDirty && !state.isBusy
        saveButton.alpha = if (saveButton.isEnabled) 1f else DISABLED_ALPHA
    }

    /** The picked photo, else the account's photo, else the initial of the name. */
    private fun showPhoto(state: EditProfileUiState) {
        binding.avatarInitial.text = state.name.trim().firstOrNull()?.uppercase()
        val photo: Any? = state.pickedAvatar ?: state.user?.avatarUrl
        if (photo == shownPhoto) return
        shownPhoto = photo
        binding.avatarImage.isVisible = photo != null
        // A photo that fails to load leaves the initial on show.
        if (photo != null) binding.avatarImage.load(photo) { listener(onError = { _, _ -> binding.avatarImage.isVisible = false }) }
    }

    private fun handle(event: EditProfileEvent) {
        when (event) {
            EditProfileEvent.Saved -> {
                makeToast(R.string.edit_profile_saved)
                finish()
            }
            EditProfileEvent.ImageFailed -> makeToast(R.string.edit_profile_image_failed)
            is EditProfileEvent.SaveFailed -> makeToast(
                if (event.network) getString(R.string.auth_error_network)
                else event.message?.takeIf { it.isNotBlank() } ?: getString(R.string.auth_error_unknown),
            )
        }
    }

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard, so leave room for it ourselves.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.editScroll.updatePadding(bottom = (keyboardHeight - navigationBarHeight).coerceAtLeast(0))
        }
        if (keyboardHeight > 0) binding.editScroll.scrollToFocused(currentFocus)
    }

    companion object {
        private const val DISABLED_ALPHA = 0.4f

        fun newIntent(context: Context): Intent = Intent(context, EditProfileActivity::class.java)
    }
}
