package com.example.huetaste.feature.scan_history

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import com.example.huetaste.common.extension.startScanActivity
import com.example.huetaste.common.extension.startScanDishDetailActivity
import com.example.huetaste.databinding.ActivityScanHistoryBinding
import com.example.huetaste.domain.model.ScanHistoryFilter
import com.example.huetaste.feature.ai_chat.ChatFormat
import com.example.huetaste.feature.ai_chat.ChatItemsBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The Stitch "Lịch sử scan món" screen backed by `GET /users/me/lich-su-scan`: the user's past scans by day. */
@AndroidEntryPoint
class ScanHistoryActivity : LsActivity<ActivityScanHistoryBinding>(ActivityScanHistoryBinding::inflate) {
    private val viewModel: ScanHistoryViewModel by viewModels()

    private val scanAdapter = ScanHistoryAdapter(
        onOpen = { startScanDishDetailActivity(it.id) },
        onMore = { DeleteScanDialog.show(supportFragmentManager, it.id, it.name) },
    )

    private val filterChips
        get() = mapOf(
            ScanHistoryFilter.ALL to binding.filterAll,
            ScanHistoryFilter.HUE_SPECIALTY to binding.filterHue,
            ScanHistoryFilter.UPDATING to binding.filterUpdating,
            ScanHistoryFilter.SAVED to binding.filterSaved,
        )

    private var deletedSnackbar: Snackbar? = null

    override fun viewCreated() {
        binding.scanList.adapter = scanAdapter
        binding.scanList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && layout.findLastVisibleItemPosition() >= scanAdapter.itemCount - LOAD_MORE_AHEAD) {
                    viewModel.loadMore()
                }
            }
        })

        binding.backButton.clicks { handleBackPressed() }
        binding.retryButton.clicks { viewModel.reload() }
        binding.scanButton.clicks { startScanActivity() }
        binding.firstScanButton.clicks { startScanActivity() }
        binding.showAllButton.clicks { viewModel.selectFilter(ScanHistoryFilter.ALL) }
        filterChips.forEach { (filter, chip) -> chip.clicks { viewModel.selectFilter(filter) } }

        supportFragmentManager.setFragmentResultListener(DeleteScanDialog.REQUEST_KEY, this) { _, result ->
            result.getString(DeleteScanDialog.RESULT_ID)?.let(viewModel::delete)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    /** Back from a new scan or a dish (which may have been saved): the history may have changed. */
    override fun onRestart() {
        super.onRestart()
        viewModel.reload()
    }

    private fun render(state: ScanHistoryUiState) = with(binding) {
        filterChips.forEach { (filter, chip) -> chip.isSelected = filter == state.filter }
        filterAll.text = if (state.filter == ScanHistoryFilter.ALL && !state.isLoading && state.error == null) {
            getString(R.string.scan_history_filter_all_count, state.total)
        } else {
            getString(R.string.scan_history_filter_all)
        }

        scanAdapter.submitList(buildItems(state))
        val error = state.error
        loadingProgress.isVisible = state.isLoading
        errorView.isVisible = !state.isLoading && error != null
        val showList = !state.isLoading && error == null
        scanList.isVisible = showList && state.items.isNotEmpty()
        val isEmpty = showList && state.items.isEmpty()
        emptyView.isVisible = isEmpty && state.filter == ScanHistoryFilter.ALL
        filterEmptyView.isVisible = isEmpty && state.filter != ScanHistoryFilter.ALL
    }

    /** Groups the scans by "Hôm nay", "Hôm qua" or their date, newest first as the backend sends them. */
    private fun buildItems(state: ScanHistoryUiState): List<ScanHistoryUiItem> {
        val items = state.items.groupBy { ChatItemsBuilder.dayStart(it.createdAt) }.flatMap { (dayStart, scans) ->
            listOf<ScanHistoryUiItem>(
                ScanHistoryUiItem.Header(dayLabel(dayStart), getString(R.string.scan_history_day_count, scans.size)),
            ) + scans.map { ScanHistoryUiItem.Card(it, ChatFormat.time(it.createdAt)) }
        }
        return if (state.isLoadingMore) items + ScanHistoryUiItem.LoadingMore else items
    }

    private fun dayLabel(dayStart: Long): String = when (ChatFormat.daysAgo(dayStart)) {
        0 -> getString(R.string.ai_chat_today)
        1 -> getString(R.string.ai_chat_yesterday)
        else -> ChatFormat.date(dayStart)
    }

    private fun onEvent(event: ScanHistoryEvent) {
        when (event) {
            ScanHistoryEvent.LoadFailed -> makeToast(R.string.scan_history_error_message)
            ScanHistoryEvent.ActionFailed -> makeToast(R.string.scan_history_action_failed)
            is ScanHistoryEvent.Deleted -> {
                deletedSnackbar?.dismiss()
                deletedSnackbar = Snackbar.make(binding.root, R.string.scan_history_deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.scanButton)
                    .setActionTextColor(ContextCompat.getColor(this, R.color.hue_secondary_container))
                    .setAction(R.string.favorites_undo) { viewModel.undoDelete(event.scanId) }
                    .also { it.show() }
            }
        }
    }

    override fun onDestroy() {
        deletedSnackbar = null
        super.onDestroy()
    }

    companion object {
        /** Rows left below the last visible one when the next page starts loading. */
        private const val LOAD_MORE_AHEAD = 4

        fun newIntent(context: Context): Intent = Intent(context, ScanHistoryActivity::class.java)
    }
}
