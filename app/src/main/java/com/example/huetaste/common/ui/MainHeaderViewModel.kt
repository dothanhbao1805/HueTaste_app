package com.example.huetaste.common.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.model.UserProfile
import com.example.huetaste.domain.usecase.GetCurrentAreaUseCase
import com.example.huetaste.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainHeaderState(
    /** Null while the device's area is being found. */
    val area: DeviceArea? = null,
    val user: UserProfile? = null,
)

/** What the top bar of the tabs without a screen model of their own needs: the device's area and the person. */
@HiltViewModel
class MainHeaderViewModel @Inject constructor(
    private val getCurrentArea: GetCurrentAreaUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainHeaderState())
    val uiState: StateFlow<MainHeaderState> = _uiState.asStateFlow()

    /** Opens the device's location settings, once the permission was just granted and location is still off. */
    private val _openSettings = Channel<Unit>(Channel.BUFFERED)
    val openSettings = _openSettings.receiveAsFlow()

    private var locationJob: Job? = null

    init {
        refreshLocation()
        viewModelScope.launch {
            getCurrentUser.observe().filterNotNull().collect { user -> _uiState.update { it.copy(user = user) } }
        }
    }

    /** @param openSettingsIfOff true right after the permission was granted, when device location is the last step. */
    fun refreshLocation(openSettingsIfOff: Boolean = false) {
        locationJob?.cancel()
        _uiState.update { it.copy(area = null) }
        locationJob = viewModelScope.launch {
            val area = getCurrentArea()
            _uiState.update { it.copy(area = area) }
            if (area == DeviceArea.LocationOff && openSettingsIfOff) _openSettings.send(Unit)
        }
    }
}
