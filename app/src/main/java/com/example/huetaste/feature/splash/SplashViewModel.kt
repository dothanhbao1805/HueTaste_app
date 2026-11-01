package com.example.huetaste.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the launch screen sends the user, decided from what is remembered about the last session. */
enum class SplashDestination { MAIN, LOGIN }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getAuthState: GetAuthStateUseCase,
) : ViewModel() {

    private val _destination = Channel<SplashDestination>(Channel.BUFFERED)
    val destination = _destination.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Reading the stored session runs in parallel with the minimum time the brand stays on screen.
            val state = async { getAuthState() }
            delay(MIN_SPLASH_MS)
            // A signed-in user or a returning guest never sees the login again.
            _destination.send(
                when (state.await()) {
                    AuthState.SIGNED_IN, AuthState.GUEST -> SplashDestination.MAIN
                    AuthState.SIGNED_OUT -> SplashDestination.LOGIN
                },
            )
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 1_200L
    }
}
