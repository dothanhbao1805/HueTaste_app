package com.example.huetaste.feature.taste_setup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.FragmentTasteDietBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Step 1: the diets the backend filters dishes by; picking none means eating everything. */
@AndroidEntryPoint
class DietStepFragment : LsFragment<FragmentTasteDietBinding>(FragmentTasteDietBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(requireActivity())[TasteSetupViewModel::class.java] }

    override fun onViewCreated() {
        binding.status.root.clicks { viewModel.loadOptions() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TasteSetupUiState) {
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
        binding.dietList.bindOptions(listOf(normal) + diets) { option ->
            if (option.key == NORMAL_KEY) viewModel.selectNormalDiet() else viewModel.toggleDiet(option.key)
        }
        binding.status.render(state.optionsState, state.diets.isEmpty())
    }

    private companion object {
        /** Not a backend id: the backend has no "normal" diet, it is the empty selection. */
        const val NORMAL_KEY = ""
    }
}
