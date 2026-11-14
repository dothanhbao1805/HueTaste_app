package com.example.huetaste.feature.place_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsBottomSheet
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.applySheetHeight
import com.example.huetaste.databinding.ItemReportDishChipBinding
import com.example.huetaste.databinding.ItemReportReasonBinding
import com.example.huetaste.databinding.SheetReportPlaceBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.PlaceDetail
import kotlinx.coroutines.launch

/** Why the user reports a place; [requiresDescription] makes the description mandatory. */
enum class ReportReason(@param:StringRes val label: Int, val requiresDescription: Boolean = false) {
    WRONG_INFO(R.string.place_report_reason_wrong_info),
    CLOSED(R.string.place_report_reason_closed),
    WRONG_MENU(R.string.place_report_reason_wrong_menu),
    WRONG_PRICE(R.string.place_report_reason_wrong_price),
    INAPPROPRIATE(R.string.place_report_reason_inappropriate),
    OTHER(R.string.place_report_reason_other, requiresDescription = true),
}

/**
 * Lets the user report wrong information about the place: one reason, the wrong dishes of its menu and a description.
 * The place, its menu and the submit progress live in [PlaceDetailViewModel], which posts `POST /bao-cao`.
 */
class ReportPlaceSheet : LsBottomSheet<SheetReportPlaceBinding>(SheetReportPlaceBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(requireActivity())[PlaceDetailViewModel::class.java] }

    private var reason: ReportReason? = null
    private val selectedDishIds = linkedSetOf<String>()
    private var shownDetail: PlaceDetail? = null

    override fun isFull(): Boolean = false

    // The description box must stay above the keyboard.
    override fun isAdjustResize(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { state ->
            reason = state.getString(KEY_REASON)?.let { name -> ReportReason.entries.firstOrNull { it.name == name } }
            state.getStringArrayList(KEY_DISHES)?.let(selectedDishIds::addAll)
        }
    }

    override fun onViewCreated() {
        binding.root.applySheetHeight()
        bindReasons()
        showReason()
        binding.reportCloseButton.clicks { dismiss() }
        binding.reportSubmitButton.clicks { submit() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (state is PlaceDetailUiState.Content) showPlace(state.content.detail)
                    }
                }
                launch { viewModel.reportState.collect(::render) }
            }
        }
    }

    private fun bindReasons() {
        val inflater = LayoutInflater.from(requireContext())
        binding.reportReasons.removeAllViews()
        ReportReason.entries.forEach { item ->
            val row = ItemReportReasonBinding.inflate(inflater, binding.reportReasons, false)
            row.reasonTitle.setText(item.label)
            row.root.contentDescription = getString(item.label)
            row.root.clicks { selectReason(item) }
            binding.reportReasons.addView(row.root)
        }
    }

    private fun selectReason(value: ReportReason) {
        reason = value
        showReason()
        binding.reportError.isVisible = false
    }

    private fun showReason() {
        ReportReason.entries.forEachIndexed { index, item ->
            val row = ItemReportReasonBinding.bind(binding.reportReasons.getChildAt(index))
            val selected = item == reason
            row.root.isSelected = selected
            row.reasonCheck.isSelected = selected
        }
        binding.reportInput.setHint(
            if (reason?.requiresDescription == true) R.string.place_report_description_hint_required
            else R.string.place_report_description_hint,
        )
    }

    private fun showPlace(detail: PlaceDetail) {
        if (shownDetail === detail) return
        shownDetail = detail
        binding.reportPlaceName.text = detail.place.name
        val dishes = detail.menu.map { it.dish }.distinctBy { it.id }
        // A selection restored from a previous menu must not keep dishes the place no longer serves.
        selectedDishIds.retainAll(dishes.map { it.id }.toSet())
        binding.reportDishesSection.isVisible = dishes.isNotEmpty()
        binding.reportDishes.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        dishes.forEach { dish ->
            val chip = ItemReportDishChipBinding.inflate(inflater, binding.reportDishes, false).root
            chip.text = dish.name
            chip.isSelected = dish.id in selectedDishIds
            chip.clicks {
                if (!selectedDishIds.remove(dish.id)) selectedDishIds.add(dish.id)
                chip.isSelected = dish.id in selectedDishIds
            }
            binding.reportDishes.addView(chip)
        }
    }

    private fun submit() {
        val selected = reason
        val description = binding.reportInput.text?.toString()?.trim().orEmpty()
        val error = when {
            selected == null -> R.string.place_report_reason_required
            selected.requiresDescription && description.isEmpty() -> R.string.place_report_description_required
            else -> null
        }
        if (error != null) {
            binding.reportError.setText(error)
            binding.reportError.isVisible = true
            return
        }
        binding.reportError.isVisible = false
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.reportInput.windowToken, 0)
        val chosen = selected ?: return
        viewModel.submitReport(getString(chosen.label), description, selectedDishIds.toList())
    }

    private fun render(state: ReportSubmitState) {
        val submitting = state is ReportSubmitState.Submitting
        binding.reportSubmitButton.isEnabled = !submitting
        binding.reportSubmitButton.alpha = if (submitting) DISABLED_ALPHA else 1f
        binding.reportInput.isEnabled = !submitting
        when (state) {
            is ReportSubmitState.Failed -> {
                binding.reportError.text = errorMessage(state.error)
                binding.reportError.isVisible = true
                viewModel.acknowledgeReportResult()
            }
            ReportSubmitState.Submitted -> {
                viewModel.acknowledgeReportResult()
                dismiss()
            }
            else -> Unit
        }
    }

    private fun errorMessage(error: AppError): String = when (error.code) {
        AppError.REPORT_PENDING -> getString(R.string.place_report_pending)
        AppError.DISH_NOT_IN_PLACE -> getString(R.string.place_report_dish_not_in_place)
        else -> error.message?.takeIf { it.isNotBlank() } ?: getString(R.string.place_report_failed)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_REASON, reason?.name)
        outState.putStringArrayList(KEY_DISHES, ArrayList(selectedDishIds))
    }

    companion object {
        private const val TAG = "report_place"
        private const val KEY_REASON = "report_reason"
        private const val KEY_DISHES = "report_dishes"
        private const val DISABLED_ALPHA = 0.5f

        fun isShown(fragmentManager: FragmentManager) = fragmentManager.findFragmentByTag(TAG) != null

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            ReportPlaceSheet().show(fragmentManager, TAG)
        }
    }
}
