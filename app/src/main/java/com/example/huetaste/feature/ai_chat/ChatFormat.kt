package com.example.huetaste.feature.ai_chat

import android.content.Context
import com.example.huetaste.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Display formatting shared by the chat cards. */
internal object ChatFormat {
    private val vietnamese = Locale("vi", "VN")

    /** Hour and minute, for example `11:42`; empty when the backend sent no readable time. */
    fun time(millis: Long): String =
        if (millis <= 0L) "" else SimpleDateFormat("HH:mm", vietnamese).format(Date(millis))

    fun date(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", vietnamese).format(Date(millis))

    /** "Hôm nay", "Hôm qua", or the date, for the day that starts at [dayStart]. */
    fun dayLabel(context: Context, dayStart: Long): String = when (daysAgo(dayStart)) {
        0 -> context.getString(R.string.ai_chat_today)
        1 -> context.getString(R.string.ai_chat_yesterday)
        else -> date(dayStart)
    }

    /** Whole days between the day that starts at [dayStart] and today. */
    fun daysAgo(dayStart: Long): Int {
        val today = ChatItemsBuilder.dayStart(System.currentTimeMillis())
        return ((today - dayStart) / DAY_MS).toInt()
    }

    private const val DAY_MS = 24 * 60 * 60 * 1000L
}
