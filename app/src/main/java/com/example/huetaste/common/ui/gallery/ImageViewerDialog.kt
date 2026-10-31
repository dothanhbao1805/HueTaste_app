package com.example.huetaste.common.ui.gallery

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.base.common.base.LsDialogFragment
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.DialogImageViewerBinding
import dagger.hilt.android.AndroidEntryPoint

/** Full-screen photo viewer with swipe between photos and zoom. Reports the last viewed page when closed. */
@AndroidEntryPoint
class ImageViewerDialog : LsDialogFragment<DialogImageViewerBinding>(DialogImageViewerBinding::inflate, true) {
    private var currentPage = 0
    private var isRecreated = false

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPage = position
            showCounter()
        }
    }

    init {
        // The viewer is always dark; the base class would paint the bars with the light theme background.
        autoColorSystemBar = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isRecreated = savedInstanceState != null
        currentPage = savedInstanceState?.getInt(KEY_PAGE) ?: requireArguments().getInt(ARG_START_INDEX)
    }

    override fun onViewCreated() {
        val urls = requireArguments().getStringArrayList(ARG_URLS).orEmpty()
        showDarkSystemBars()
        applyInsets()

        binding.viewerPager.adapter = ImageViewerAdapter(onSingleTap = ::toggleChrome).also { it.submitList(urls) }
        binding.viewerPager.registerOnPageChangeCallback(pageCallback)
        if (!isRecreated) binding.viewerPager.setCurrentItem(currentPage, false)
        binding.viewerCloseButton.clicks { dismiss() }
        showCounter()
    }

    private fun showDarkSystemBars() {
        val window = dialog?.window ?: return
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    /** Moves the controls clear of the system bars; margins are recomputed from their layout values, never added up. */
    private fun applyInsets() {
        val closeTop = (binding.viewerCloseButton.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val counterBottom = (binding.viewerCounter.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.viewerCloseButton.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = closeTop + bars.top }
            binding.viewerCounter.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = counterBottom + bars.bottom }
            insets
        }
    }

    private fun showCounter() {
        val total = binding.viewerPager.adapter?.itemCount ?: 0
        binding.viewerCounter.isVisible = total > 1 && binding.viewerCloseButton.isVisible
        binding.viewerCounter.text = getString(R.string.dish_image_counter, currentPage + 1, total)
    }

    private fun toggleChrome() {
        binding.viewerCloseButton.isVisible = !binding.viewerCloseButton.isVisible
        showCounter()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, currentPage)
    }

    override fun onDestroyView() {
        binding.viewerPager.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(RESULT_PAGE to currentPage))
    }

    companion object {
        const val RESULT_KEY = "image_viewer_result"
        const val RESULT_PAGE = "page"
        private const val TAG = "image_viewer"
        private const val ARG_URLS = "urls"
        private const val ARG_START_INDEX = "start_index"
        private const val KEY_PAGE = "viewer_page"

        fun show(fragmentManager: FragmentManager, urls: List<String>, startIndex: Int) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            ImageViewerDialog().apply {
                arguments = bundleOf(ARG_URLS to ArrayList(urls), ARG_START_INDEX to startIndex)
            }.show(fragmentManager, TAG)
        }
    }
}
