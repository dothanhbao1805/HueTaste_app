package com.example.huetaste.feature.home

import android.os.Bundle
import android.view.View
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.FragmentHomeBinding
import com.example.huetaste.feature.context_suggestion.ContextSuggestionActivity
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : LsFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {
    private var selectedMealIndex = DEFAULT_MEAL_INDEX
    private var selectedHearts = DEFAULT_HEARTS.toMutableSet()

    private val budgetFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))

    private val mealChips get() = listOf(
        binding.mealBreakfast,
        binding.mealLunch,
        binding.mealDinner,
        binding.mealSnacks
    )

    private val hearts get() = listOf(
        binding.heartBeef,
        binding.heartBanhBeo,
        binding.heartComHen
    )

    private val budgetListener = Slider.OnChangeListener { _, value, _ -> showBudget(value) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { state ->
            selectedMealIndex = state.getInt(KEY_MEAL, DEFAULT_MEAL_INDEX)
            selectedHearts = state.getIntArray(KEY_HEARTS)?.toMutableSet() ?: DEFAULT_HEARTS.toMutableSet()
        }
    }

    override fun onViewCreated() {
        mealChips.forEachIndexed { index, chip ->
            chip.isSelected = index == selectedMealIndex
            chip.clicks(withAnim = false) { selectMeal(index) }
        }
        hearts.forEachIndexed { index, heart ->
            heart.isSelected = index in selectedHearts
            heart.clicks { toggleHeart(index, heart) }
        }
        showBudget(binding.budgetSlider.value)
        binding.budgetSlider.addOnChangeListener(budgetListener)

        binding.suggestButton.clicks {
            startActivity(ContextSuggestionActivity.newIntent(requireContext()))
        }

        binding.scanDishButton.clicks {
            // Action to trigger dish camera scanner
        }
    }

    private fun selectMeal(index: Int) {
        selectedMealIndex = index
        mealChips.forEachIndexed { chipIndex, chip -> chip.isSelected = chipIndex == index }
    }

    private fun toggleHeart(index: Int, heart: View) {
        val selected = !heart.isSelected
        heart.isSelected = selected
        if (selected) selectedHearts.add(index) else selectedHearts.remove(index)
    }

    private fun showBudget(value: Float) {
        binding.budgetValue.text = getString(R.string.home_budget_format, budgetFormat.format(value.toLong()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_MEAL, selectedMealIndex)
        outState.putIntArray(KEY_HEARTS, selectedHearts.toIntArray())
    }

    override fun onDestroyView() {
        binding.budgetSlider.removeOnChangeListener(budgetListener)
        super.onDestroyView()
    }

    private companion object {
        const val KEY_MEAL = "home_selected_meal"
        const val KEY_HEARTS = "home_selected_hearts"
        const val DEFAULT_MEAL_INDEX = 0
        val DEFAULT_HEARTS = setOf(0)
    }
}
