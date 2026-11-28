package com.example.huetaste.common.ui

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.startNotificationsActivity
import com.example.huetaste.databinding.ViewMainHeaderBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetUnreadNotificationCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The unread count behind the header bell; one instance per Activity, shared by every tab. */
@HiltViewModel
class NotificationBellViewModel @Inject constructor(
    private val getAuthState: GetAuthStateUseCase,
    private val getUnreadCount: GetUnreadNotificationCountUseCase,
) : ViewModel() {

    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    private var job: Job? = null

    /** All tabs start together, so overlapping calls share one request. */
    fun refresh() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            try {
                _unread.value = if (getAuthState() == AuthState.SIGNED_IN) getUnreadCount() else 0
            } catch (e: AppError) {
                // Offline: keep the last known count rather than hiding unread notifications.
            }
        }
    }
}

/** Opens the notifications from the header bell and shows a dot while some are unread. */
fun Fragment.bindNotificationBell(header: ViewMainHeaderBinding) {
    val viewModel = ViewModelProvider(requireActivity())[NotificationBellViewModel::class.java]
    header.notificationsButton.clicks { requireContext().startNotificationsActivity() }
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Read again on every return, for example from the notifications screen.
            viewModel.refresh()
            viewModel.unread.collect { count ->
                header.notificationsDot.isVisible = count > 0
                header.notificationsButton.contentDescription =
                    if (count > 0) getString(R.string.notifications_unread_description, count)
                    else getString(R.string.home_notifications)
            }
        }
    }
}
