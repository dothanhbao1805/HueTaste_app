package com.example.huetaste.feature.daily_needs

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.ui.DiscardChangesDialog
import com.example.huetaste.databinding.ActivityDailyNeedsBinding
import com.example.huetaste.domain.model.MealBudget
import com.example.huetaste.domain.model.MealType
import com.example.huetaste.feature.taste_setup.OptionsState
import com.example.huetaste.feature.taste_setup.TasteOption
import com.example.huetaste.feature.taste_setup.bindOptions
import com.example.huetaste.feature.taste_setup.icon
import com.example.huetaste.feature.taste_setup.label
import com.example.huetaste.feature.taste_setup.render
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Stitch "Nhu cầu hôm nay" screen: the user picks today's meals, the budget per meal and a taste note. */
@AndroidEntryPoint
class DailyNeedsActivity : LsActivity<ActivityDailyNeedsBinding>(ActivityDailyNeedsBinding::inflate) {
    private val viewModel: DailyNeedsViewModel by viewModels()

    private val budgetTiles
        get() = mapOf(
            MealBudget.UNDER_30K to binding.budgetUnder30,
            MealBudget.FROM_30K_TO_60K to binding.budget30To60,
            MealBudget.FROM_60K_TO_100K to binding.budget60To100,
            MealBudget.OVER_100K to binding.budgetOver100,
        )

    private var isDirty = false

    override fun viewCreated() {
        binding.backButton.clicks { handleBackPressed() }
        binding.saveButton.clicks { viewModel.save() }
        binding.clearButton.clicks { viewModel.clearAll() }
        binding.status.root.clicks { viewModel.load() }
        binding.noteNoSpicy.clicks { viewModel.appendToNote(getString(R.string.daily_needs_note_no_spicy)) }
        binding.noteVegetarian.clicks { viewModel.appendToNote(getString(R.string.daily_needs_note_vegetarian)) }
        budgetTiles.forEach { (budget, tile) ->
            tile.budgetLabel.setText(budget.label)
            tile.budgetHint.setText(budget.hint)
            tile.root.clicks { viewModel.selectBudget(budget) }
        }
        binding.noteInput.doAfterTextChanged { viewModel.setNote(it?.toString().orEmpty()) }

        supportFragmentManager.setFragmentResultListener(DiscardChangesDialog.REQUEST_KEY, this) { _, _ -> finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            DailyNeedsEvent.Saved -> {
                                makeToast(R.string.daily_needs_saved)
                                finish()
                            }
                            is DailyNeedsEvent.SaveFailed ->
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

    private fun render(state: DailyNeedsUiState) = with(binding) {
        isDirty = state.isDirty
        status.render(state.optionsState, isEmpty = false)
        content.isVisible = state.optionsState == OptionsState.READY

        val meals = MealType.entries.map { meal ->
            TasteOption(meal.name, getString(meal.label), getString(meal.hint), meal.icon, meal in state.meals)
        }
        mealList.bindOptions(meals) { option -> viewModel.toggleMeal(MealType.valueOf(option.key)) }
        mealCount.text = getString(R.string.taste_meals_count, state.meals.size)

        budgetTiles.forEach { (budget, tile) ->
            val selected = budget == state.budget
            tile.root.isSelected = selected
            tile.budgetCheck.isInvisible = !selected
        }
        budgetSummary.setText(state.budget?.label ?: R.string.daily_needs_budget_none)

        // Only text the user did not type (a quick phrase, a reload, "Xóa lựa chọn") is pushed into the field.
        if (noteInput.text?.toString() != state.note) {
            noteInput.setText(state.note)
            noteInput.setSelection(state.note.length)
        }
        noteCounter.text = getString(R.string.daily_needs_note_counter, state.note.length, DailyNeedsViewModel.NOTE_MAX_LENGTH)

        saveButton.isEnabled = state.isDirty && !state.isSaving
        saveButton.alpha = if (saveButton.isEnabled) 1f else DISABLED_ALPHA
        saveButton.setText(if (state.isSaving) R.string.taste_prefs_saving else R.string.taste_prefs_save)
        clearButton.isVisible = state.optionsState == OptionsState.READY
    }

    private val MealType.hint: Int
        @StringRes get() = when (this) {
            MealType.BREAKFAST -> R.string.daily_needs_meal_breakfast_hint
            MealType.LUNCH -> R.string.daily_needs_meal_lunch_hint
            MealType.SNACK -> R.string.daily_needs_meal_snack_hint
            MealType.DINNER -> R.string.daily_needs_meal_dinner_hint
            MealType.LATE_NIGHT -> R.string.daily_needs_meal_late_night_hint
        }

    private val MealBudget.label: Int
        @StringRes get() = when (this) {
            MealBudget.UNDER_30K -> R.string.taste_budget_under_30
            MealBudget.FROM_30K_TO_60K -> R.string.taste_budget_30_60
            MealBudget.FROM_60K_TO_100K -> R.string.taste_budget_60_100
            MealBudget.OVER_100K -> R.string.taste_budget_over_100
        }

    private val MealBudget.hint: Int
        @StringRes get() = when (this) {
            MealBudget.UNDER_30K -> R.string.daily_needs_budget_under_30_hint
            MealBudget.FROM_30K_TO_60K -> R.string.daily_needs_budget_30_60_hint
            MealBudget.FROM_60K_TO_100K -> R.string.daily_needs_budget_60_100_hint
            MealBudget.OVER_100K -> R.string.daily_needs_budget_over_100_hint
        }

    companion object {
        private const val DISABLED_ALPHA = 0.4f

        fun newIntent(context: Context): Intent = Intent(context, DailyNeedsActivity::class.java)
    }
}
