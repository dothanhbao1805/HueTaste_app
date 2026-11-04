package com.example.huetaste.feature.taste_setup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.FragmentTasteDailyNeedBinding
import com.example.huetaste.domain.model.MealBudget
import com.example.huetaste.domain.model.MealType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Step 3: today's meals, the budget per meal and the ingredients to avoid, saved as today's needs. */
@AndroidEntryPoint
class DailyNeedStepFragment : LsFragment<FragmentTasteDailyNeedBinding>(FragmentTasteDailyNeedBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(requireActivity())[TasteSetupViewModel::class.java] }

    private val budgetOptions get() = mapOf(
        MealBudget.UNDER_30K to binding.budgetUnder30,
        MealBudget.FROM_30K_TO_60K to binding.budget30To60,
        MealBudget.FROM_60K_TO_100K to binding.budget60To100,
        MealBudget.OVER_100K to binding.budgetOver100,
    )

    override fun onViewCreated() {
        binding.avoidHeader.sectionTitle.setText(R.string.taste_avoid_title)
        budgetOptions.forEach { (budget, view) -> view.clicks { viewModel.selectBudget(budget) } }

        binding.searchInput.setOnFocusChangeListener { _, hasFocus -> binding.searchField.isActivated = hasFocus }
        binding.searchInput.doAfterTextChanged { viewModel.searchIngredient(it?.toString().orEmpty()) }
        // The field restores its own text after recreation; the search follows it.
        viewModel.searchIngredient(binding.searchInput.text?.toString().orEmpty())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TasteSetupUiState) {
        val meals = MealType.entries.map { meal ->
            TasteOption(meal.name, getString(meal.label), meal.defaultTime, meal.icon, meal in state.meals)
        }
        binding.mealList.bindOptions(meals) { option -> viewModel.toggleMeal(MealType.valueOf(option.key)) }
        binding.mealCount.text = getString(R.string.taste_meals_count, state.meals.size)

        budgetOptions.forEach { (budget, view) -> view.isSelected = budget == state.budget }

        val avoidIds = state.avoidIngredients.map { it.id }.toSet()
        binding.avoidSelectedGroup.isVisible = state.avoidIngredients.isNotEmpty()
        binding.avoidSelectedGroup.bindChips(state.avoidIngredients, { it.id }, { it.name }, { true }) {
            viewModel.toggleAvoid(it)
        }
        val results = state.ingredientResults.filterNot { it.id in avoidIds }
        binding.avoidResultGroup.bindChips(results, { it.id }, { it.name }, { false }) { viewModel.toggleAvoid(it) }

        val status = when {
            state.isSearchingIngredients -> R.string.taste_avoid_searching
            results.isEmpty() && state.ingredientResults.isEmpty() -> R.string.taste_avoid_empty
            else -> null
        }
        binding.avoidStatus.isVisible = status != null
        status?.let(binding.avoidStatus::setText)
    }

    private val MealType.label: Int
        @StringRes get() = when (this) {
            MealType.BREAKFAST -> R.string.taste_meal_breakfast
            MealType.LUNCH -> R.string.taste_meal_lunch
            MealType.SNACK -> R.string.taste_meal_snack
            MealType.DINNER -> R.string.taste_meal_dinner
            MealType.LATE_NIGHT -> R.string.taste_meal_late_night
        }

    private val MealType.icon: Int
        @DrawableRes get() = when (this) {
            MealType.BREAKFAST -> R.drawable.ic_wb_twilight
            MealType.LUNCH -> R.drawable.ic_wb_sunny
            MealType.SNACK -> R.drawable.ic_icecream
            MealType.DINNER -> R.drawable.ic_restaurant_menu
            MealType.LATE_NIGHT -> R.drawable.ic_bedtime
        }
}
