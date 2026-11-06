package com.example.huetaste.feature.ai_chat

import android.content.Context
import com.example.huetaste.R
import com.example.huetaste.domain.model.OpeningHours
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Display formatting shared by the chat cards. */
internal object ChatFormat {
    private val vietnamese = Locale("vi", "VN")

    /** `giaThamKhaoTu` / `giaThamKhaoDen` (or a place's `mucGia`) as VND, or null when neither is known. */
    fun price(context: Context, from: Int?, to: Int?): String? {
        val format = NumberFormat.getIntegerInstance(vietnamese)
        return when {
            from != null && to != null && from != to ->
                context.getString(R.string.dish_price_range, format.format(from), format.format(to))
            from != null -> context.getString(R.string.dish_price_value, format.format(from))
            to != null -> context.getString(R.string.dish_price_value, format.format(to))
            else -> null
        }
    }

    /** `khoangCachKm` as meters below 1 km, otherwise as kilometers with one decimal. */
    fun distance(context: Context, km: Double): String = if (km < 1) {
        context.getString(R.string.ai_chat_distance_m, (km * 1000).roundToInt())
    } else {
        val format = NumberFormat.getNumberInstance(vietnamese).apply { maximumFractionDigits = 1 }
        context.getString(R.string.ai_chat_distance_km, format.format(km))
    }

    /** Whether the place is open now according to its `gioMoCua` lines, or null when they cannot be read. */
    fun isOpenNow(openingHours: List<String>): Boolean? {
        val now = Calendar.getInstance()
        // Calendar counts Sunday as 1; OpeningHours counts Monday as 1 and Sunday as 7.
        val day = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return OpeningHours.forDay(openingHours, day, minute)?.isOpenNow
    }

    fun rating(value: Double): String = String.format(Locale.US, "%.1f", value)

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
