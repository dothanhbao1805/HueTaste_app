package com.example.huetaste.feature.my_reports

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.startMainTab
import com.example.huetaste.databinding.ActivityMyReportsBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.feature.main.MainTab
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Stitch "Báo cáo của tôi" screen backed by `GET /users/me/bao-cao`: the user's place reports and how they were handled. */
@AndroidEntryPoint
class MyReportsActivity : LsActivity<ActivityMyReportsBinding>(ActivityMyReportsBinding::inflate) {
    private val viewModel: MyReportsViewModel by viewModels()

    private val reportAdapter = MyReportAdapter()

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.reportList.adapter = reportAdapter
        binding.reportList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layout.findLastVisibleItemPosition() >= reportAdapter.itemCount - LOAD_MORE_AHEAD) {
                    viewModel.loadMore()
                }
            }
        })
        binding.emptyMessage.text = emptyMessage()

        binding.backButton.clicks { handleBackPressed() }
        binding.retryButton.clicks { viewModel.reload() }
        binding.exploreButton.clicks { startMainTab(MainTab.HOME) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            MyReportsEvent.LoadMoreFailed -> makeToast(R.string.my_reports_error_message)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: MyReportsUiState) {
        val cards = state.reports.map(MyReportUiItem::Card)
        reportAdapter.submitList(if (state.isLoadingMore) cards + MyReportUiItem.LoadingMore else cards)
        val error = state.error
        binding.loadingProgress.isVisible = state.isLoading
        binding.errorView.isVisible = !state.isLoading && error != null
        binding.errorHint.isVisible = error?.code == AppError.NETWORK
        val showList = !state.isLoading && error == null
        binding.reportList.isVisible = showList && state.reports.isNotEmpty()
        binding.emptyView.isVisible = showList && state.reports.isEmpty()
    }

    /** The empty message with the place screen's report link name highlighted. */
    private fun emptyMessage(): CharSequence {
        val link = getString(R.string.place_report_link)
        val parts = getString(R.string.my_reports_empty_message, link).split(link, limit = 2)
        return buildSpannedString {
            append(parts.first())
            if (parts.size == 2) {
                bold { color(ContextCompat.getColor(this@MyReportsActivity, R.color.hue_primary)) { append(link) } }
                append(parts[1])
            }
        }
    }

    companion object {
        /** Rows left below the last visible one when the next page starts loading. */
        private const val LOAD_MORE_AHEAD = 4

        fun newIntent(context: Context): Intent = Intent(context, MyReportsActivity::class.java)
    }
}
