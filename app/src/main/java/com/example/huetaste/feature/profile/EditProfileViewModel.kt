package com.example.huetaste.feature.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.common.ui.ImageFileProcessor
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.usecase.GetCurrentUserUseCase
import com.example.huetaste.domain.usecase.UpdateNameUseCase
import com.example.huetaste.domain.usecase.UploadAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EditProfileUiState(
    /** Null until the signed-in user has been read. */
    val user: UserProfile? = null,
    val name: String = "",
    /** The photo picked but not uploaded yet, already resized. */
    val pickedAvatar: File? = null,
    val isPreparingImage: Boolean = false,
    val isSaving: Boolean = false,
    val nameInvalid: Boolean = false,
) {
    val isDirty get() = user != null && (pickedAvatar != null || name.trim() != user.name)
    val isBusy get() = isPreparingImage || isSaving
}

sealed interface EditProfileEvent {
    object Saved : EditProfileEvent
    object ImageFailed : EditProfileEvent
    data class SaveFailed(val network: Boolean, val message: String?) : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val updateName: UpdateNameUseCase,
    private val uploadAvatar: UploadAvatarUseCase,
    private val imageProcessor: ImageFileProcessor,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    // The picked photo survives process death as long as its cached file does.
    private val _uiState = MutableStateFlow(
        EditProfileUiState(pickedAvatar = savedState.get<String>(KEY_AVATAR)?.let(::File)?.takeIf { it.exists() }),
    )
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            getCurrentUser.observe().filterNotNull().collect { user ->
                _uiState.update {
                    // The name starts from the account once; later changes to it must not undo what is being typed.
                    val name = if (it.user == null) savedState.get<String>(KEY_NAME) ?: user.name else it.name
                    it.copy(user = user, name = name)
                }
            }
        }
    }

    fun setName(name: String) {
        if (name == _uiState.value.name) return
        savedState[KEY_NAME] = name
        _uiState.update { it.copy(name = name, nameInvalid = false) }
    }

    fun pickAvatar(uri: Uri) {
        if (job?.isActive == true) return
        _uiState.update { it.copy(isPreparingImage = true) }
        job = viewModelScope.launch {
            val file = try {
                imageProcessor.fromGallery(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Decoding and writing can fail in many ways; each means this photo cannot be used.
                null
            }
            if (file != null) setPickedAvatar(file) else _events.send(EditProfileEvent.ImageFailed)
            _uiState.update { it.copy(isPreparingImage = false) }
        }
    }

    /** Uploads the new photo, then the new name; what succeeded is not sent again if the other step fails. */
    fun save() {
        if (job?.isActive == true) return
        val state = _uiState.value
        val user = state.user ?: return
        if (!AuthValidator.isNameValid(state.name)) {
            _uiState.update { it.copy(nameInvalid = true) }
            return
        }
        if (!state.isDirty) return
        val name = state.name.trim()
        _uiState.update { it.copy(isSaving = true) }
        job = viewModelScope.launch {
            try {
                state.pickedAvatar?.let { file ->
                    uploadAvatar(file)
                    setPickedAvatar(null)
                }
                if (name != user.name) updateName(name)
                _events.send(EditProfileEvent.Saved)
            } catch (e: AppError) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(EditProfileEvent.SaveFailed(e.code == AppError.NETWORK, e.message))
            }
        }
    }

    private fun setPickedAvatar(file: File?) {
        savedState[KEY_AVATAR] = file?.path
        _uiState.update { it.copy(pickedAvatar = file) }
    }

    private companion object {
        const val KEY_NAME = "edit_profile_name"
        const val KEY_AVATAR = "edit_profile_avatar"
    }
}
