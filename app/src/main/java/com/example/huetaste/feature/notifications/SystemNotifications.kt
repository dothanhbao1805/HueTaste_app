package com.example.huetaste.feature.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.huetaste.R
import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.feature.main.MainActivity

/** Puts notifications in the status bar under the app's single channel. */
object SystemNotifications {
    private const val CHANNEL_ID = "huetaste_updates"

    // Checked through areNotificationsEnabled(); a permission revoked meanwhile is caught below.
    @SuppressLint("MissingPermission")
    fun show(context: Context, items: List<AppNotification>) {
        val manager = NotificationManagerCompat.from(context)
        if (items.isEmpty() || !manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notifications_channel_name))
                .setDescription(context.getString(R.string.notifications_channel_description))
                .build(),
        )
        items.forEach { item ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(ContextCompat.getColor(context, R.color.hue_primary))
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
                .setAutoCancel(true)
                .setContentIntent(openIntent(context, item))
                .build()
            try {
                manager.notify(item.id.hashCode(), notification)
            } catch (e: SecurityException) {
                return
            }
        }
    }

    /** The main screen stays under the notifications list, so Back from there does not leave the app. */
    private fun openIntent(context: Context, item: AppNotification): PendingIntent = PendingIntent.getActivities(
        context,
        item.id.hashCode(),
        arrayOf(MainActivity.newRootIntent(context), NotificationsActivity.newIntent(context, item)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
