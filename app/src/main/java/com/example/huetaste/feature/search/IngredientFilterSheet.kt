package com.example.huetaste.feature.search

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsBottomSheet
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.applySheetHeight
import com.example.huetaste.common.ui.bindChips
import com.example.huetaste.databinding.SheetSearchIngredientsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Picks the ingredients a dish must all contain; "Áp dụng" hands them back through [RESULT_KEY]. */
@AndroidEntryPoint
class IngredientFilterSheet :
    LsBottomSheet<SheetSearchIngredientsBinding>(SheetSearchIngredientsBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[IngredientFilterViewModel::class.java] }

    override fun isFull(): Boolean = false

    override fun isAdjustResize(): Boolean = true

    override fun onViewCreated() {
        with(binding) {
            root.applySheetHeight()
            ingredientClose.clicks { dismiss() }
            ingredientSearchInput.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
            ingredientReset.clicks { viewModel.reset() }
            ingredientApply.clicks {
                val selected = ArrayList(viewModel.uiState.value.selected)
                parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(RESULT_INGREDIENTS to selected))
                dismiss()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: IngredientFilterUiState) = with(binding) {
        ingredientSelectedGroup.isVisible = state.selected.isNotEmpty()
        ingredientSelectedGroup.bindChips(state.selected, { it }, { it }, { true }) { viewModel.toggle(it) }

        val results = state.results.map { it.name }.distinct().filterNot { it in state.selected }
        ingredientResultGroup.bindChips(results, { it }, { it }, { false }) { viewModel.toggle(it) }
        ingredientStatus.isVisible = state.isSearching || results.isEmpty()
        ingredientStatus.setText(
            when {
                state.isSearching -> R.string.search_ingredients_searching
                state.failed -> R.string.search_ingredients_failed
                else -> R.string.search_ingredients_empty
            },
        )
        ingredientApply.text = if (state.selected.isEmpty()) {
            getString(R.string.search_apply)
        } else {
            getString(R.string.search_apply_count, state.selected.size)
        }
    }

    companion object {
        const val RESULT_KEY = "search_ingredients_result"
        const val RESULT_INGREDIENTS = "ingredients"
        const val ARG_SELECTED = "selected"
        private const val TAG = "IngredientFilterSheet"

        fun show(manager: FragmentManager, selected: List<String>) {
            if (manager.findFragmentByTag(TAG) != null) return
            IngredientFilterSheet()
                .apply { arguments = bundleOf(ARG_SELECTED to ArrayList(selected)) }
                .show(manager, TAG)
        }
    }
}
