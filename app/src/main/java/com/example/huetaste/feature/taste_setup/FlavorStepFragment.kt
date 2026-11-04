package com.example.huetaste.feature.taste_setup

import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.FragmentTasteFlavorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Step 2: spiciness plus the flavours and dish styles the user likes. */
@AndroidEntryPoint
class FlavorStepFragment : LsFragment<FragmentTasteFlavorBinding>(FragmentTasteFlavorBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(requireActivity())[TasteSetupViewModel::class.java] }

    private val spiceSegments get() = mapOf(
        SpiceLevel.NONE to binding.spiceNone,
        SpiceLevel.MILD to binding.spiceMild,
        SpiceLevel.MEDIUM to binding.spiceMedium,
        SpiceLevel.HUE to binding.spiceHue,
    )

    override fun onViewCreated() {
        binding.flavorHeader.sectionTitle.setText(R.string.taste_flavor_section)
        binding.styleHeader.sectionTitle.setText(R.string.taste_style_section)
        spiceSegments.forEach { (level, segment) -> segment.clicks { viewModel.selectSpice(level) } }
        binding.status.root.clicks { viewModel.loadOptions() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TasteSetupUiState) {
        binding.spiceSection.isVisible = state.spicyTag != null
        spiceSegments.forEach { (level, segment) -> segment.isSelected = level == state.spiceLevel }

        binding.flavorSection.isVisible = state.flavorTags.isNotEmpty()
        binding.flavorGroup.bindChips(state.flavorTags, { it.id }, { it.name }, { it.id in state.selectedTagIds }) {
            viewModel.toggleTag(it.id)
        }
        binding.styleSection.isVisible = state.styleTags.isNotEmpty()
        binding.styleGroup.bindChips(state.styleTags, { it.id }, { it.name }, { it.id in state.selectedTagIds }) {
            viewModel.toggleTag(it.id)
        }

        val isEmpty = state.spicyTag == null && state.flavorTags.isEmpty() && state.styleTags.isEmpty()
        binding.status.render(state.optionsState, isEmpty)
    }
}
