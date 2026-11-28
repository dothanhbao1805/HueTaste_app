package com.example.huetaste.feature.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.common.extension.startMainTab
import com.example.huetaste.common.extension.startMyReportsActivity
import com.example.huetaste.common.ui.ListSpacingDecoration
import com.example.huetaste.databinding.ActivityNotificationsBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.model.NotificationType
import com.example.huetaste.feature.main.MainTab
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The user's notifications, newest first; tapping one marks it read and opens what it is about. */
@AndroidEntryPoint
class NotificationsActivity : LsActivity<ActivityNotificationsBinding>(ActivityNotificationsBinding::inflate) {
    private val viewModel: NotificationsViewModel by viewModels()

    private val adapter = NotificationAdapter(onClick = { viewModel.open(it) })

    private var openedFromStatusBar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // A notification tapped in the status bar is handled once, not again after recreation.
        openedFromStatusBar = savedInstanceState == null && intent.hasExtra(EXTRA_ID)
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.notificationList.adapter = adapter
        binding.notificationList.addItemDecoration(
            ListSpacingDecoration(1, resources.getDimensionPixelSize(R.dimen._10dp)),
        )
        binding.notificationList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layout.findLastVisibleItemPosition() >= adapter.itemCount - LOAD_MORE_AHEAD) {
                    viewModel.loadMore()
                }
            }
        })
        binding.backButton.clicks { handleBackPressed() }
        binding.markAllButton.clicks { viewModel.markAllRead() }
        binding.messageButton.clicks {
            val state = viewModel.uiState.value
            when {
                state.requiresSignIn -> startLoginActivity()
                state.error != null -> viewModel.reload()
                else -> startMainTab(MainTab.HOME)
            }
        }

        if (openedFromStatusBar) openFromStatusBar()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The user is looking at the list now, so the copies in the status bar are no longer needed.
        NotificationManagerCompat.from(this).cancelAll()
    }

    private fun openFromStatusBar() {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE)?.let { name -> NotificationType.entries.firstOrNull { it.name == name } }
        viewModel.markRead(id)
        openTarget(type ?: NotificationType.OTHER, intent.getStringExtra(EXTRA_DISH_ID))
    }

    private fun render(state: NotificationsUiState) = with(binding) {
        adapter.submitList(state.items)
        loadingProgress.isVisible = state.isLoading
        val error = state.error
        val ready = !state.isLoading && !state.requiresSignIn && error == null
        notificationList.isVisible = ready && state.items.isNotEmpty()
        markAllButton.isVisible = ready && state.hasUnread

        val message = when {
            state.isLoading -> null
            state.requiresSignIn -> Message(
                R.drawable.ic_person, R.string.notifications_guest_title, R.string.notifications_guest_message, R.string.login_submit,
            )
            error != null -> Message(
                R.drawable.ic_cloud_off,
                R.string.my_reports_error_title,
                if (error.code == AppError.NETWORK) R.string.my_reports_error_hint else R.string.notifications_error_message,
                R.string.my_reports_retry,
            )
            state.items.isEmpty() -> Message(
                R.drawable.ic_notifications, R.string.notifications_empty_title, R.string.notifications_empty_message, R.string.my_reports_explore,
            )
            else -> null
        }
        messageView.isVisible = message != null
        if (message != null) {
            messageIcon.setImageResource(message.icon)
            messageTitle.setText(message.title)
            messageText.setText(message.text)
            messageButton.setText(message.button)
        }
    }

    private fun handle(event: NotificationsEvent) {
        when (event) {
            is NotificationsEvent.Open -> openTarget(event.type, event.dishId)
            NotificationsEvent.LoadMoreFailed -> makeToast(R.string.notifications_load_failed)
            NotificationsEvent.MarkAllFailed -> makeToast(R.string.notifications_mark_all_failed)
        }
    }

    private fun openTarget(type: NotificationType, dishId: String?) {
        when (type) {
            NotificationType.REPORT -> startMyReportsActivity()
            NotificationType.SCANNED_DISH, NotificationType.WEATHER ->
                if (dishId != null) startDishDetailActivity(dishId) else startMainTab(MainTab.HOME)
            NotificationType.MEAL_REMINDER, NotificationType.LUNAR_DAY -> startMainTab(MainTab.HOME)
            NotificationType.OTHER -> Unit
        }
    }

    private class Message(val icon: Int, val title: Int, val text: Int, val button: Int)

    companion object {
        private const val EXTRA_ID = "notification_id"
        private const val EXTRA_TYPE = "notification_type"
        private const val EXTRA_DISH_ID = "notification_dish_id"

        /** Rows left below the last visible one when the next page starts loading. */
        private const val LOAD_MORE_AHEAD = 4

        fun newIntent(context: Context): Intent = Intent(context, NotificationsActivity::class.java)

        /** Opens the list and then where [notification] leads, as tapping it in the list does. */
        fun newIntent(context: Context, notification: AppNotification): Intent = newIntent(context)
            .putExtra(EXTRA_ID, notification.id)
            .putExtra(EXTRA_TYPE, notification.type.name)
            .putExtra(EXTRA_DISH_ID, notification.dishId)
    }
}
