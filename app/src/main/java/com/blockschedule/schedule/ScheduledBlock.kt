package com.blockschedule.schedule

import com.blockschedule.data.Category

/**
 * A concrete time block for one day, produced by [Scheduler].
 * [startMinute]/[endMinute] are clipped to the day's timeline (0..1440).
 */
data class ScheduledBlock(
    val taskId: Long,
    val title: String,
    val category: Category,
    val startMinute: Int,
    val endMinute: Int,
    val isFlexible: Boolean,
    /** True when this block overlaps another fixed block (shown as a warning). */
    val hasConflict: Boolean = false,
    /** True for a flexible task that couldn't fit anywhere in its window today. */
    val unscheduled: Boolean = false,
    /** True when the block continues from the previous day (e.g. sleep across midnight). */
    val continuedFromYesterday: Boolean = false,
    /** True when the block runs past midnight into tomorrow. */
    val continuesTomorrow: Boolean = false
) {
    val durationMinutes: Int get() = endMinute - startMinute

    fun contains(minuteOfDay: Int): Boolean =
        !unscheduled && minuteOfDay >= startMinute && minuteOfDay < endMinute

    companion object {
        fun formatTime(minuteOfDay: Int): String {
            val m = ((minuteOfDay % 1440) + 1440) % 1440
            val h24 = m / 60
            val min = m % 60
            val ampm = if (h24 < 12) "AM" else "PM"
            val h12 = when (val h = h24 % 12) { 0 -> 12; else -> h }
            return "%d:%02d %s".format(h12, min, ampm)
        }

        fun formatRange(start: Int, end: Int): String =
            "${formatTime(start)} – ${formatTime(end)}"
    }
}
