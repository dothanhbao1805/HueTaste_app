package com.example.huetaste.feature.place_detail

import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsBottomSheet
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.SheetWriteReviewBinding
import kotlinx.coroutines.launch

/** Lets the user rate the place from 1 to 5 stars and add a comment. State lives in [PlaceDetailViewModel]. */
class ReviewSheet : LsBottomSheet<SheetWriteReviewBinding>(SheetWriteReviewBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(requireActivity())[PlaceDetailViewModel::class.java] }
    private var rating = 0

    private val stars get() = with(binding) { listOf(sheetStar1, sheetStar2, sheetStar3, sheetStar4, sheetStar5) }

    override fun isFull(): Boolean = false

    // The comment box must stay above the keyboard.
    override fun isAdjustResize(): Boolean = true

    override fun onViewCreated() {
        rating = savedRating ?: 0
        savedRating = null
        stars.forEachIndexed { index, star -> star.clicks { selectRating(index + 1) } }
        selectRating(rating)
        binding.reviewCloseButton.clicks { dismiss() }
        binding.reviewSubmitButton.clicks { submit() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reviewState.collect(::render)
            }
        }
    }

    private fun selectRating(value: Int) {
        rating = value
        bindStars(stars, value.toDouble())
        binding.reviewError.isVisible = false
    }

    private fun submit() {
        if (rating == 0) {
            binding.reviewError.setText(R.string.place_review_rating_required)
            binding.reviewError.isVisible = true
            return
        }
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.reviewInput.windowToken, 0)
        viewModel.submitReview(rating, binding.reviewInput.text?.toString().orEmpty())
    }

    private fun render(state: ReviewSubmitState) {
        val submitting = state is ReviewSubmitState.Submitting
        binding.reviewSubmitButton.isEnabled = !submitting
        binding.reviewSubmitButton.alpha = if (submitting) DISABLED_ALPHA else 1f
        binding.reviewInput.isEnabled = !submitting
        when (state) {
            is ReviewSubmitState.Failed -> {
                binding.reviewError.text = state.error.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.place_review_failed)
                binding.reviewError.isVisible = true
                viewModel.acknowledgeReviewResult()
            }
            ReviewSubmitState.Submitted -> {
                viewModel.acknowledgeReviewResult()
                dismiss()
            }
            else -> Unit
        }
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_RATING, rating)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        savedRating = savedInstanceState?.getInt(KEY_RATING)
    }

    private var savedRating: Int? = null

    companion object {
        private const val TAG = "write_review"
        private const val KEY_RATING = "review_rating"
        private const val DISABLED_ALPHA = 0.5f

        fun isShown(fragmentManager: FragmentManager) = fragmentManager.findFragmentByTag(TAG) != null

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            ReviewSheet().show(fragmentManager, TAG)
        }
    }
}
