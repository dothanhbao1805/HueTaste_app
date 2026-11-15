package com.example.huetaste.feature.my_reports

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.base.common.base.LsMultiListAdapter
import com.base.common.base.LsViewHolder
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemMyReportBinding
import com.example.huetaste.databinding.ItemMyReportDishBinding
import com.example.huetaste.databinding.ItemMyReportLoadingBinding
import com.example.huetaste.domain.model.PlaceReport
import com.example.huetaste.domain.model.ReportDishResult
import com.example.huetaste.domain.model.ReportStatus

/** One row of the my reports list: a report card or the next page loading. */
sealed interface MyReportUiItem {
    val key: String

    data class Card(val report: PlaceReport) : MyReportUiItem {
        override val key = "report_${report.id}"
    }

    object LoadingMore : MyReportUiItem {
        override val key = "loading_more"
    }
}

class MyReportAdapter : LsMultiListAdapter<MyReportUiItem, ViewBinding>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MyReportUiItem.Card -> TYPE_CARD
        MyReportUiItem.LoadingMore -> TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LsViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewBinding = when (viewType) {
            TYPE_LOADING -> ItemMyReportLoadingBinding.inflate(inflater, parent, false)
            else -> ItemMyReportBinding.inflate(inflater, parent, false)
        }
        return LsViewHolder(binding)
    }

    override fun bindItem(item: MyReportUiItem, binding: ViewBinding, position: Int) {
        if (item is MyReportUiItem.Card) bindCard(binding as ItemMyReportBinding, item)
    }

    private fun bindCard(binding: ItemMyReportBinding, item: MyReportUiItem.Card) = with(binding) {
        val context = root.context
        val report = item.report
        val resolved = report.status == ReportStatus.RESOLVED

        placeName.text = report.placeName
        placeHiddenTag.isVisible = report.placeHidden

        statusChip.backgroundTintList =
            ContextCompat.getColorStateList(context, if (resolved) R.color.hue_success_container else R.color.hue_secondary_fixed)
        val statusColor = if (resolved) R.color.hue_success else R.color.hue_on_secondary_fixed_variant
        statusIcon.setImageResource(if (resolved) R.drawable.ic_check_circle else R.drawable.ic_schedule)
        statusIcon.imageTintList = ContextCompat.getColorStateList(context, statusColor)
        statusText.setText(if (resolved) R.string.my_reports_status_resolved else R.string.my_reports_status_pending)
        statusText.setTextColor(ContextCompat.getColor(context, statusColor))

        reasonText.text = report.reason
        descriptionText.isVisible = !report.description.isNullOrBlank()
        descriptionText.text = report.description

        dishesSection.isVisible = report.dishes.isNotEmpty()
        dishList.removeAllViews()
        val inflater = LayoutInflater.from(context)
        report.dishes.forEach { dish ->
            val row = ItemMyReportDishBinding.inflate(inflater, dishList, true)
            row.dishName.text = dish.dishName
            val style = DishStyle.of(dish.result)
            row.root.backgroundTintList = ContextCompat.getColorStateList(context, style.background)
            row.resultIcon.setImageResource(style.icon)
            row.resultIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, style.content))
            row.resultText.setText(style.label)
            row.resultText.setTextColor(ContextCompat.getColor(context, style.content))
        }

        responseBlock.isVisible = resolved && !report.resolutionNote.isNullOrBlank()
        responseText.text = report.resolutionNote

        val sent = context.getString(R.string.my_reports_sent_on, displayDate(report.createdAt))
        val resolvedAt = report.resolvedAt
        dateIcon.isVisible = !resolved
        reviewingText.isVisible = !resolved
        dateText.text = if (resolved && resolvedAt != null) {
            buildSpannedString {
                append(sent).append(" · ")
                bold { color(ContextCompat.getColor(context, R.color.hue_success)) { append(context.getString(R.string.my_reports_resolved_on, displayDate(resolvedAt))) } }
            }
        } else {
            sent
        }
    }

    /** Colors, icon and label of one dish verdict. */
    private enum class DishStyle(
        @param:ColorRes val background: Int,
        @param:ColorRes val content: Int,
        @param:DrawableRes val icon: Int,
        @param:StringRes val label: Int,
    ) {
        PENDING(R.color.hue_surface_container, R.color.hue_on_surface_variant, R.drawable.ic_hourglass_empty, R.string.my_reports_dish_pending),
        VIOLATION(R.color.hue_error_container, R.color.hue_error, R.drawable.ic_cancel, R.string.my_reports_dish_violation),
        NO_VIOLATION(R.color.hue_success_container_soft, R.color.hue_success, R.drawable.ic_check, R.string.my_reports_dish_no_violation);

        companion object {
            fun of(result: ReportDishResult) = when (result) {
                ReportDishResult.PENDING -> PENDING
                ReportDishResult.VIOLATION -> VIOLATION
                ReportDishResult.NO_VIOLATION -> NO_VIOLATION
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MyReportUiItem>() {
        override fun areItemsTheSame(oldItem: MyReportUiItem, newItem: MyReportUiItem) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: MyReportUiItem, newItem: MyReportUiItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_CARD = 0
        const val TYPE_LOADING = 1

        /** "2026-11-15T08:30:00" -> "15/11/2026"; anything else is shown as sent. */
        fun displayDate(iso: String?): String {
            val parts = iso.orEmpty().take(10).split('-')
            return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso.orEmpty()
        }
    }
}
