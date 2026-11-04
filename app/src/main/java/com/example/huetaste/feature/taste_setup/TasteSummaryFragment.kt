package com.example.huetaste.feature.taste_setup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsFragment
import com.example.huetaste.R
import com.example.huetaste.databinding.FragmentTasteSummaryBinding
import com.example.huetaste.databinding.ItemTasteSummaryRowBinding
import com.example.huetaste.domain.model.MealBudget
import com.example.huetaste.domain.model.MealType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The last page: what was saved, with the way back to edit it or on to the main screen in the footer. */
@AndroidEntryPoint
class TasteSummaryFragment : LsFragment<FragmentTasteSummaryBinding>(FragmentTasteSummaryBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(requireActivity())[TasteSetupViewModel::class.java] }

    override fun onViewCreated() {
        binding.summaryTitle.text = buildSpannedString {
            append(getString(R.string.taste_summary_title_lead))
            append(' ')
            color(ContextCompat.getColor(requireContext(), R.color.hue_primary)) {
                append(getString(R.string.taste_summary_title_accent))
            }
        }
        binding.rowDiet.setup(R.drawable.ic_eco, R.string.taste_summary_diet)
        binding.rowSpice.setup(R.drawable.ic_local_fire_department, R.string.taste_summary_spice)
        binding.rowFlavor.setup(R.drawable.ic_favorite, R.string.taste_summary_flavor)
        binding.rowMeals.setup(R.drawable.ic_schedule, R.string.taste_summary_meals)
        binding.rowBudget.setup(R.drawable.ic_payments, R.string.taste_summary_budget)
        binding.rowAvoid.setup(R.drawable.ic_grocery, R.string.taste_summary_avoid)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TasteSetupUiState) {
        val none = getString(R.string.taste_summary_none)
        binding.rowDiet.rowValue.text = state.selectedDiets.joinToString(", ") { it.name }
            .ifEmpty { getString(R.string.taste_diet_normal) }

        binding.rowSpice.root.isVisible = state.spicyTag != null
        binding.rowSpice.rowValue.setText(
            when (state.spiceLevel) {
                SpiceLevel.NONE -> R.string.taste_spice_none
                SpiceLevel.MILD -> R.string.taste_spice_mild
                SpiceLevel.MEDIUM -> R.string.taste_spice_medium
                SpiceLevel.HUE -> R.string.taste_spice_hue
            },
        )

        binding.rowFlavor.rowValue.text = state.selectedTags.joinToString(", ") { it.name }.ifEmpty { none }

        binding.rowMeals.rowValue.text = state.meals.sortedBy { it.ordinal }
            .joinToString(" · ") { getString(it.shortLabel) }
            .ifEmpty { none }

        binding.rowBudget.rowValue.setText(
            when (state.budget) {
                null -> R.string.taste_summary_no_budget
                MealBudget.UNDER_30K -> R.string.taste_budget_under_30
                MealBudget.FROM_30K_TO_60K -> R.string.taste_budget_30_60
                MealBudget.FROM_60K_TO_100K -> R.string.taste_budget_60_100
                MealBudget.OVER_100K -> R.string.taste_budget_over_100
            },
        )

        binding.rowAvoid.rowValue.text = state.avoidIngredients.joinToString(", ") { it.name }.ifEmpty { none }
    }

    private fun ItemTasteSummaryRowBinding.setup(@DrawableRes icon: Int, @StringRes label: Int) {
        rowIcon.setImageResource(icon)
        rowLabel.setText(label)
    }

    private val MealType.shortLabel: Int
        @StringRes get() = when (this) {
            MealType.BREAKFAST -> R.string.taste_meal_breakfast_short
            MealType.LUNCH -> R.string.taste_meal_lunch_short
            MealType.SNACK -> R.string.taste_meal_snack_short
            MealType.DINNER -> R.string.taste_meal_dinner_short
            MealType.LATE_NIGHT -> R.string.taste_meal_late_night_short
        }
}
