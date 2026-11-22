package com.example.huetaste.feature.taste_preferences

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.common.ui.bindChips
import com.example.huetaste.R
import com.example.huetaste.databinding.ActivityTastePreferencesBinding
import com.example.huetaste.feature.taste_setup.OptionsState
import com.example.huetaste.feature.taste_setup.SpiceLevel
import com.example.huetaste.feature.taste_setup.render
import com.example.huetaste.common.ui.DiscardChangesDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Stitch "Sở thích ẩm thực" screen: the user edits spiciness, liked flavours and dish styles. */
@AndroidEntryPoint
class TastePreferencesActivity : LsActivity<ActivityTastePreferencesBinding>(ActivityTastePreferencesBinding::inflate) {
    private val viewModel: TastePreferencesViewModel by viewModels()

    private val spiceSegments
        get() = mapOf(
            SpiceLevel.NONE to binding.spiceNone,
            SpiceLevel.MILD to binding.spiceMild,
            SpiceLevel.MEDIUM to binding.spiceMedium,
            SpiceLevel.HUE to binding.spiceHue,
        )

    private var isDirty = false

    override fun viewCreated() {
        binding.backButton.clicks { handleBackPressed() }
        binding.saveButton.clicks { viewModel.save() }
        binding.resetButton.clicks { viewModel.reset() }
        binding.status.root.clicks { viewModel.load() }
        spiceSegments.forEach { (level, segment) -> segment.clicks { viewModel.selectSpice(level) } }

        supportFragmentManager.setFragmentResultListener(DiscardChangesDialog.REQUEST_KEY, this) { _, _ -> finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            TastePreferencesEvent.Saved -> {
                                makeToast(R.string.taste_prefs_saved)
                                finish()
                            }
                            is TastePreferencesEvent.SaveFailed ->
                                makeToast(if (event.network) R.string.taste_prefs_network_failed else R.string.taste_save_failed)
                        }
                    }
                }
            }
        }
    }

    override fun handleBackPressed() {
        if (isDirty) DiscardChangesDialog.show(supportFragmentManager) else finish()
    }

    private fun render(state: TastePreferencesUiState) = with(binding) {
        isDirty = state.isDirty
        val ready = state.optionsState == OptionsState.READY
        val isEmpty = state.spicyTag == null && state.flavorTags.isEmpty() && state.styleTags.isEmpty()
        status.render(state.optionsState, isEmpty)

        val none = getString(R.string.taste_summary_none)
        spiceCard.isVisible = ready && state.spicyTag != null
        spiceSegments.forEach { (level, segment) -> segment.isSelected = level == state.spiceLevel }
        spiceSummary.setText(spiceLabel(state.spiceLevel))

        flavorCard.isVisible = ready && state.flavorTags.isNotEmpty()
        flavorGroup.bindChips(state.flavorTags, { it.id }, { it.name }, { it.id in state.selectedTagIds }) {
            viewModel.toggleTag(it.id)
        }
        flavorSummary.text = state.selectedFlavors.joinToString(", ") { it.name }.ifEmpty { none }

        styleCard.isVisible = ready && state.styleTags.isNotEmpty()
        styleGroup.bindChips(state.styleTags, { it.id }, { it.name }, { it.id in state.selectedTagIds }) {
            viewModel.toggleTag(it.id)
        }
        styleSummary.text = state.selectedStyles.joinToString(", ") { it.name }.ifEmpty { none }

        resetButton.isVisible = ready && state.isDirty
        saveButton.isEnabled = state.isDirty && !state.isSaving
        saveButton.alpha = if (saveButton.isEnabled) 1f else DISABLED_ALPHA
        saveButton.setText(if (state.isSaving) R.string.taste_prefs_saving else R.string.taste_prefs_save)
    }

    private fun spiceLabel(level: SpiceLevel) = when (level) {
        SpiceLevel.NONE -> R.string.taste_spice_none
        SpiceLevel.MILD -> R.string.taste_spice_mild
        SpiceLevel.MEDIUM -> R.string.taste_spice_medium
        SpiceLevel.HUE -> R.string.taste_spice_hue
    }

    companion object {
        private const val DISABLED_ALPHA = 0.4f

        fun newIntent(context: Context): Intent = Intent(context, TastePreferencesActivity::class.java)
    }
}
