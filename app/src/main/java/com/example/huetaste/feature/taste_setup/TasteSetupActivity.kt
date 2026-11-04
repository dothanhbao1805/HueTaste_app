package com.example.huetaste.feature.taste_setup

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.base.LsPageAdapter
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.startMainActivity
import com.example.huetaste.databinding.ActivityTasteSetupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Sets up the user's taste after registering (or after a login without any saved taste): diet, flavours, today's
 * needs and a summary. The pages share [TasteSetupViewModel]; they change only through the footer and Back.
 */
@AndroidEntryPoint
class TasteSetupActivity : LsActivity<ActivityTasteSetupBinding>(ActivityTasteSetupBinding::inflate) {
    private val viewModel: TasteSetupViewModel by viewModels()

    private val stepBars get() = listOf(binding.stepBar1, binding.stepBar2, binding.stepBar3)

    private var shownStep: TasteStep? = null

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.viewPager.adapter = LsPageAdapter(supportFragmentManager, lifecycle).apply {
            addFragments(DietStepFragment(), FlavorStepFragment(), DailyNeedStepFragment(), TasteSummaryFragment())
        }
        binding.viewPager.offscreenPageLimit = TasteStep.entries.size - 1
        binding.viewPager.isUserInputEnabled = false

        binding.backButton.clicks { handleBackPressed() }
        binding.skipButton.clicks { viewModel.skip() }
        binding.primaryButton.clicks { viewModel.continueStep() }
        binding.secondaryButton.clicks {
            if (viewModel.uiState.value.step == TasteStep.SUMMARY) viewModel.editAgain() else viewModel.skip()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
            }
        }
    }

    override fun handleBackPressed() = viewModel.back()

    private fun render(state: TasteSetupUiState) {
        val step = state.step
        if (shownStep != step) {
            // The first page after a launch or recreation appears without the slide.
            binding.viewPager.setCurrentItem(step.ordinal, shownStep != null)
            shownStep = step
        }

        val isSummary = step == TasteStep.SUMMARY
        binding.progressBlock.isInvisible = isSummary
        binding.skipButton.isInvisible = isSummary
        binding.stepText.text = getString(R.string.taste_step, step.ordinal + 1, stepBars.size)
        stepBars.forEachIndexed { index, bar ->
            bar.backgroundTintList = getColorStateList(
                if (index <= step.ordinal) R.color.hue_primary else R.color.hue_surface_container_high,
            )
        }

        binding.primaryText.setText(
            when (step) {
                TasteStep.DIET, TasteStep.FLAVOR -> R.string.taste_continue
                TasteStep.DAILY_NEED -> R.string.taste_finish
                TasteStep.SUMMARY -> R.string.taste_explore
            },
        )
        binding.primaryArrow.isVisible = !state.isSaving && step >= TasteStep.DAILY_NEED
        binding.primarySpinner.isVisible = state.isSaving
        binding.primaryButton.isEnabled = !state.isSaving
        binding.primaryButton.alpha = if (state.isSaving) LOADING_ALPHA else 1f

        val secondary = when (step) {
            TasteStep.DAILY_NEED -> R.string.taste_later
            TasteStep.SUMMARY -> R.string.taste_edit_again
            else -> null
        }
        binding.secondaryButton.isVisible = secondary != null
        secondary?.let(binding.secondaryButton::setText)
        binding.secondaryButton.isEnabled = !state.isSaving
    }

    private fun handle(event: TasteSetupEvent) {
        when (event) {
            TasteSetupEvent.OpenMain -> {
                startMainActivity()
                finish()
            }
            is TasteSetupEvent.Failed -> makeToast(
                if (event.network) {
                    getString(R.string.auth_error_network)
                } else {
                    event.message?.takeIf { it.isNotBlank() } ?: getString(R.string.taste_save_failed)
                },
            )
        }
    }

    override fun onKeyboardHeightChanged(keyboardHeight: Int) {
        // From Android 11 the window no longer resizes for the keyboard; the footer stays under it, the page above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val covered = keyboardHeight - navigationBarHeight - binding.footer.height
            binding.viewPager.updatePadding(bottom = covered.coerceAtLeast(0))
        }
    }

    companion object {
        private const val LOADING_ALPHA = 0.8f

        /** Opens the setup as the only screen, right after registering or logging in. */
        fun newRootIntent(context: Context): Intent = Intent(context, TasteSetupActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
