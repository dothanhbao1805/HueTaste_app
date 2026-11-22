package com.example.huetaste.feature.search

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsBottomSheet
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.SheetSearchRadiusBinding
import com.google.android.material.slider.Slider

/** The "Bán kính" chip: 1 to [SearchFilters.MAX_RADIUS_KM] km around the user, handed back through [RESULT_KEY]. */
class RadiusSheet : LsBottomSheet<SheetSearchRadiusBinding>(SheetSearchRadiusBinding::inflate) {

    private val radiusListener = Slider.OnChangeListener { _, value, _ -> showRadius(value.toInt()) }

    /** The slider brings back its own value after recreation; the argument is only the starting point. */
    private var restored = false

    override fun isFull(): Boolean = false

    override fun isAdjustResize(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restored = savedInstanceState != null
    }

    override fun onViewCreated() {
        with(binding) {
            radiusSlider.valueTo = SearchFilters.MAX_RADIUS_KM.toFloat()
            if (!restored) {
                radiusSlider.value = requireArguments().getInt(ARG_RADIUS, SearchFilters.DEFAULT_RADIUS_KM)
                    .coerceIn(1, SearchFilters.MAX_RADIUS_KM).toFloat()
            }
            showRadius(radiusSlider.value.toInt())
            radiusSlider.addOnChangeListener(radiusListener)
            radiusClose.clicks { dismiss() }
            radiusApply.clicks {
                parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(RESULT_RADIUS to radiusSlider.value.toInt()))
                dismiss()
            }
        }
    }

    private fun showRadius(km: Int) {
        binding.radiusValue.text = getString(R.string.search_radius_value, km)
    }

    override fun onDestroyView() {
        binding.radiusSlider.removeOnChangeListener(radiusListener)
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "search_radius_result"
        const val RESULT_RADIUS = "radius"
        private const val ARG_RADIUS = "radius"
        private const val TAG = "RadiusSheet"

        fun show(manager: FragmentManager, radiusKm: Int) {
            if (manager.findFragmentByTag(TAG) != null) return
            RadiusSheet().apply { arguments = bundleOf(ARG_RADIUS to radiusKm) }.show(manager, TAG)
        }
    }
}
