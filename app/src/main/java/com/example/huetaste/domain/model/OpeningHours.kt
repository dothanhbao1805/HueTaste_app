package com.example.huetaste.domain.model

/**
 * What a place's opening hours say about one day. [text] is the hours as the backend wrote them for that day
 * (for example `6:00–22:00`); [isOpenNow] is null when the text could not be understood.
 */
data class DayHours(val text: String, val isOpenNow: Boolean?)

/**
 * Reads the backend `gioMoCua` lines, each shaped like `Thứ Hai: 6:00–22:00` (Vietnamese or English day names,
 * several ranges separated by commas, or a word such as `Closed` / `Open 24 hours`).
 */
object OpeningHours {
    private val dayNames = listOf(
        listOf("thứ hai", "thu hai", "monday"),
        listOf("thứ ba", "thu ba", "tuesday"),
        listOf("thứ tư", "thu tu", "wednesday"),
        listOf("thứ năm", "thu nam", "thursday"),
        listOf("thứ sáu", "thu sau", "friday"),
        listOf("thứ bảy", "thu bay", "saturday"),
        listOf("chủ nhật", "chu nhat", "sunday"),
    )
    private val closedWords = listOf("closed", "đóng cửa", "dong cua", "nghỉ")
    private val allDayWords = listOf("24 hours", "24 giờ", "24 gio", "24h", "cả ngày")
    private val range = Regex(
        """(\d{1,2})(?::(\d{2}))?\s*([ap]m)?\s*(?:[–—-]|to|đến)\s*(\d{1,2})(?::(\d{2}))?\s*([ap]m)?""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * @param dayOfWeek 1 for Monday through 7 for Sunday.
     * @param minuteOfDay minutes since midnight, used for [DayHours.isOpenNow].
     * @return null when no line describes that day.
     */
    fun forDay(lines: List<String>, dayOfWeek: Int, minuteOfDay: Int): DayHours? {
        val names = dayNames.getOrNull(dayOfWeek - 1) ?: return null
        val rest = lines.firstNotNullOfOrNull { line -> restAfterDay(line.trim(), names) } ?: return null
        val lower = rest.lowercase()
        return when {
            closedWords.any { lower.contains(it) } -> DayHours(rest, isOpenNow = false)
            allDayWords.any { lower.contains(it) } -> DayHours(rest, isOpenNow = true)
            else -> {
                val ranges = range.findAll(rest).mapNotNull { toMinutes(it) }.toList()
                DayHours(rest, isOpenNow = if (ranges.isEmpty()) null else ranges.any { minuteOfDay in it })
            }
        }
    }

    private fun restAfterDay(line: String, names: List<String>): String? {
        val lower = line.lowercase()
        // Longest name first so "thứ hai" wins over a shorter prefix.
        val name = names.sortedByDescending { it.length }.firstOrNull { lower.startsWith(it) } ?: return null
        val rest = line.substring(name.length).trimStart(':', ' ', '\t')
        return rest.takeIf { it.isNotEmpty() && (line.length == name.length || line[name.length] in ": \t") }
    }

    private fun toMinutes(match: MatchResult): IntRange? {
        val (startHour, startMinute, startMeridiem, endHour, endMinute, endMeridiem) = match.readGroups()
        val end = to24Hour(endHour.toInt(), endMeridiem) ?: return null
        // "6:00–10:00 PM": the start takes the end's meridiem unless that would put it after the end.
        val startMer = startMeridiem.ifEmpty {
            if (endMeridiem.isEmpty()) "" else if (startHour.toInt() % 12 <= endHour.toInt() % 12) endMeridiem else "am"
        }
        val start = to24Hour(startHour.toInt(), startMer) ?: return null
        val startTotal = start * 60 + (startMinute.toIntOrNull() ?: 0)
        var endTotal = end * 60 + (endMinute.toIntOrNull() ?: 0)
        if (endTotal <= startTotal) endTotal += MINUTES_PER_DAY // closes after midnight
        return startTotal..endTotal
    }

    private fun to24Hour(hour: Int, meridiem: String): Int? {
        if (hour > 24) return null
        return when (meridiem.lowercase()) {
            "am" -> if (hour == 12) 0 else hour
            "pm" -> if (hour == 12) 12 else hour + 12
            else -> hour
        }
    }

    private data class Groups(
        val startHour: String,
        val startMinute: String,
        val startMeridiem: String,
        val endHour: String,
        val endMinute: String,
        val endMeridiem: String,
    )

    private fun MatchResult.readGroups(): Groups {
        fun group(index: Int) = groups[index]?.value.orEmpty()
        return Groups(group(1), group(2), group(3), group(4), group(5), group(6))
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
