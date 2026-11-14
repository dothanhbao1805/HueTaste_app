package com.example.huetaste.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.usecase.GetCurrentUserUseCase
import com.example.huetaste.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    object Guest : ProfileUiState
    data class SignedIn(val user: UserProfile) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // The user is read from the stored session, so it is loaded once; nothing on this screen changes it.
        viewModelScope.launch {
            _uiState.value = getCurrentUser()?.let(ProfileUiState::SignedIn) ?: ProfileUiState.Guest
        }
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
