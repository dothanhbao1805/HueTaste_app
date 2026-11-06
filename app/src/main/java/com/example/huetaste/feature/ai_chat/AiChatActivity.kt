package com.example.huetaste.feature.ai_chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.openMap
import com.example.huetaste.common.extension.startChatHistoryActivity
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.databinding.ActivityAiChatBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The Stitch "Trợ Lý AI Ẩm Thực" screen backed by `/tro-chuyen`. Opened from the main bottom bar it starts a new
 * conversation; the history screen reopens it with a conversation id through [onNewIntent].
 */
@AndroidEntryPoint
class AiChatActivity : LsActivity<ActivityAiChatBinding>(ActivityAiChatBinding::inflate) {
    private val viewModel: AiChatViewModel by viewModels()

    private val chatAdapter = ChatAdapter(
        onSuggestion = ::fillInput,
        onRetry = { viewModel.retry() },
        onDishClick = { dish -> startDishDetailActivity(dish.id) },
        onPlaceClick = { place -> startPlaceDetailActivity(place.id) },
        onDirections = { place -> openMap(place) },
    )

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setUsesLocation(granted)
        if (!granted) makeToast(R.string.ai_chat_location_denied)
    }

    private var shownItemCount = 0
    private var isSending = false

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.chatList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatList.adapter = chatAdapter

        binding.backButton.clicks { handleBackPressed() }
        binding.historyButton.clicks { startChatHistoryActivity() }
        binding.newChatButton.clicks {
            binding.messageInput.text?.clear()
            viewModel.newConversation()
        }
        binding.locationChip.clicks { toggleLocation() }
        binding.sendButton.clicks { send() }
        binding.signInButton.clicks { startLoginActivity() }
        binding.messageInput.doAfterTextChanged { showSendEnabled() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Back from the history screen: open the chosen conversation, or a new one.
        viewModel.open(intent.getStringExtra(EXTRA_CONVERSATION_ID))
    }

    /** Taps on the send button and the quick questions must not close the keyboard. */
    override fun isAutoHideKeyboard(): Boolean = false

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard, so lift the composer above it. LsActivity
        // resets the guideline to the navigation bar before every call, so this never adds up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.navigationBar.setGuidelineEnd(maxOf(navigationBarHeight, keyboardHeight))
        }
        if (keyboardHeight > 0) scrollToLatest()
    }

    private fun render(state: AiChatUiState) {
        isSending = state.isSending
        val grew = state.items.size > shownItemCount
        shownItemCount = state.items.size
        chatAdapter.submitList(state.items) { if (grew) scrollToLatest() }
        binding.loadingProgress.isVisible = state.isLoading
        binding.composer.isVisible = !state.requiresSignIn
        binding.signInBar.isVisible = state.requiresSignIn
        showLocation(state.usesLocation)
        showSendEnabled()
    }

    private fun send() {
        if (viewModel.send(binding.messageInput.text?.toString().orEmpty())) {
            binding.messageInput.text?.clear()
        }
    }

    private fun showSendEnabled() {
        val enabled = !isSending && !binding.messageInput.text.isNullOrBlank()
        binding.sendButton.isEnabled = enabled
        binding.sendButton.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private fun toggleLocation() {
        if (viewModel.uiState.value.usesLocation) {
            viewModel.setUsesLocation(false)
        } else if (hasLocationPermission()) {
            viewModel.setUsesLocation(true)
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun fillInput(text: String) {
        binding.messageInput.setText(text)
        binding.messageInput.setSelection(binding.messageInput.text?.length ?: 0)
        binding.messageInput.requestFocus()
    }

    /** The backend uses the location only to find places near the user; the chip shows whether it is sent. */
    private fun showLocation(usesLocation: Boolean) {
        val content = ContextCompat.getColor(this, if (usesLocation) R.color.hue_primary else R.color.hue_on_surface_variant)
        val fill = ContextCompat.getColor(this, if (usesLocation) R.color.hue_primary_10 else R.color.hue_surface_container_low)
        binding.locationChip.backgroundTintList = ColorStateList.valueOf(fill)
        binding.locationIcon.imageTintList = ColorStateList.valueOf(content)
        binding.locationText.setTextColor(content)
        binding.locationText.setText(if (usesLocation) R.string.ai_chat_location_on else R.string.ai_chat_location)
        binding.locationChip.isSelected = usesLocation
    }

    private fun scrollToLatest() {
        val last = chatAdapter.itemCount - 1
        if (last >= 0) binding.chatList.post { binding.chatList.scrollToPosition(last) }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val DISABLED_ALPHA = 0.4f

        /**
         * Opens the chat on [conversationId], or on a new conversation. An open chat is brought back instead of
         * starting a second one.
         */
        fun newIntent(context: Context, conversationId: String? = null): Intent =
            Intent(context, AiChatActivity::class.java)
                .putExtra(EXTRA_CONVERSATION_ID, conversationId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
