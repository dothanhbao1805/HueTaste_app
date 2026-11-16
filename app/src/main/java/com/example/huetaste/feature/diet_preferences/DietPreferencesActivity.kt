package com.example.huetaste.feature.diet_preferences

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
import com.example.huetaste.R
import com.example.huetaste.common.ui.DiscardChangesDialog
import com.example.huetaste.databinding.ActivityDietPreferencesBinding
import com.example.huetaste.feature.taste_setup.OptionsState
import com.example.huetaste.feature.taste_setup.TasteOption
import com.example.huetaste.feature.taste_setup.bindOptions
import com.example.huetaste.feature.taste_setup.render
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Stitch "Chế độ ăn" screen: the user edits the diets suggestions are filtered by. */
@AndroidEntryPoint
class DietPreferencesActivity : LsActivity<ActivityDietPreferencesBinding>(ActivityDietPreferencesBinding::inflate) {
    private val viewModel: DietPreferencesViewModel by viewModels()

    private var isDirty = false

    override fun viewCreated() {
        binding.backButton.clicks { handleBackPressed() }
        binding.saveButton.clicks { viewModel.save() }
        binding.status.root.clicks { viewModel.load() }

        supportFragmentManager.setFragmentResultListener(DiscardChangesDialog.REQUEST_KEY, this) { _, _ -> finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            DietPreferencesEvent.Saved -> {
                                makeToast(R.string.diet_prefs_saved)
                                finish()
                            }
                            is DietPreferencesEvent.SaveFailed ->
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

    private fun render(state: DietPreferencesUiState) = with(binding) {
        isDirty = state.isDirty
        val ready = state.optionsState == OptionsState.READY
        status.render(state.optionsState, state.diets.isEmpty())
        dietList.isVisible = ready
        infoBox.isVisible = ready

        val normal = TasteOption(
            key = NORMAL_KEY,
            title = getString(R.string.taste_diet_normal),
            subtitle = getString(R.string.taste_diet_normal_description),
            icon = R.drawable.ic_restaurant,
            selected = state.selectedDietIds.isEmpty(),
        )
        val diets = state.diets.map { diet ->
            TasteOption(diet.id, diet.name, diet.description, R.drawable.ic_eco, diet.id in state.selectedDietIds)
        }
        dietList.bindOptions(listOf(normal) + diets) { option ->
            if (option.key == NORMAL_KEY) viewModel.selectNormal() else viewModel.toggleDiet(option.key)
        }

        saveButton.isEnabled = state.isDirty && !state.isSaving
        saveButton.alpha = if (saveButton.isEnabled) 1f else DISABLED_ALPHA
        saveButton.setText(if (state.isSaving) R.string.taste_prefs_saving else R.string.taste_prefs_save)
    }

    companion object {
        /** Not a backend id: the backend has no "normal" diet, it is the empty selection. */
        private const val NORMAL_KEY = ""
        private const val DISABLED_ALPHA = 0.4f

        fun newIntent(context: Context): Intent = Intent(context, DietPreferencesActivity::class.java)
    }
}
