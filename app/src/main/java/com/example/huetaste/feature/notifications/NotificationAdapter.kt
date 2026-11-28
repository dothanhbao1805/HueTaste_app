package com.example.huetaste.feature.notifications

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.base.common.base.LsListAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemNotificationBinding
import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit,
) : LsListAdapter<AppNotification, ItemNotificationBinding>(DiffCallback, ItemNotificationBinding::inflate) {

    override fun bindItem(item: AppNotification, binding: ItemNotificationBinding, position: Int) = with(binding) {
        val context = root.context
        typeIcon.setImageResource(item.type.icon)
        titleText.text = item.title
        bodyText.text = item.body
        bodyText.isVisible = item.body.isNotBlank()
        timeText.text = displayTime(context, item.createdAt)
        unreadDot.isVisible = !item.isRead
        root.backgroundTintList = ContextCompat.getColorStateList(
            context,
            if (item.isRead) R.color.hue_surface_lowest else R.color.hue_secondary_fixed_50,
        )
        root.contentDescription = listOfNotNull(
            context.getString(R.string.notifications_unread).takeIf { !item.isRead },
            item.title,
            item.body,
            timeText.text,
        ).joinToString(". ")
        root.clicks { onClick(item) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification) = oldItem == newItem
    }

    companion object {
        private val backendTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        /** "Hôm nay, 07:05" for today, else the date and time; the backend sends its local (Vietnam) time. */
        fun displayTime(context: Context, iso: String?): String {
            val date = iso?.take(19)?.let { runCatching { backendTime.parse(it) }.getOrNull() } ?: return ""
            val day = SimpleDateFormat("yyyyMMdd", Locale.US)
            val time = SimpleDateFormat("HH:mm", Locale.US).format(date)
            return if (day.format(date) == day.format(Date())) context.getString(R.string.notifications_today, time)
            else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date) + " " + time
        }
    }
}

@get:DrawableRes
val NotificationType.icon: Int
    get() = when (this) {
        NotificationType.REPORT -> R.drawable.ic_flag
        NotificationType.SCANNED_DISH -> R.drawable.ic_document_scanner
        NotificationType.MEAL_REMINDER -> R.drawable.ic_schedule
        NotificationType.LUNAR_DAY -> R.drawable.ic_eco
        NotificationType.WEATHER -> R.drawable.ic_thermostat
        NotificationType.OTHER -> R.drawable.ic_notifications
    }
