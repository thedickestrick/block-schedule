package com.blockschedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * A task the user has defined. The scheduling engine expands these into concrete
 * time blocks for any given day (see com.blockschedule.schedule.Scheduler).
 *
 * Two shapes of task:
 *  - Fixed-time  (isFixedTime = true):  starts at [startMinute] for [durationMinutes].
 *                                        e.g. Work 09:00-17:00, Sleep 23:00-07:00.
 *  - Flexible    (isFixedTime = false): no set start; the engine drops it into the first
 *                                        open gap inside [windowStartMinute]..[windowEndMinute].
 *                                        e.g. "30 min reading" somewhere in the afternoon.
 *
 * All minute values are minutes from local midnight (0..1439). A block may run past
 * midnight (e.g. sleep); the engine clips it to the day being displayed.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val category: Category = Category.OTHER,

    val isFixedTime: Boolean = true,
    /** Start time for fixed-time tasks, minutes from midnight. Ignored when flexible. */
    val startMinute: Int = 9 * 60,
    val durationMinutes: Int = 60,

    val frequency: Frequency = Frequency.DAILY,
    /** Bitmask of weekdays for WEEKLY/BIWEEKLY. Bit (DayOfWeek.value - 1): Mon=bit0 .. Sun=bit6. */
    val daysOfWeek: Int = 0,
    /**
     * How many times, for count-based frequencies:
     *  - TIMES_PER_WEEK: number of (auto-spread) days per week.
     *  - TIMES_PER_DAY: number of (flexible, spread) instances each day.
     */
    val count: Int = 1,
    /**
     * If set, this task is a sub-block nested inside the parent task (e.g. a lunch break
     * inside Work). It occurs whenever the parent occurs and is shown nested under it.
     */
    val parentId: Long? = null,
    /**
     * Reference date as epoch-day.
     *  - ONCE: the date the task happens.
     *  - Recurring: the first day the task is active (also sets the parity for BIWEEKLY,
     *    the day-of-month for MONTHLY, and the month+day for YEARLY).
     */
    val anchorEpochDay: Long,

    /** Earliest start for flexible tasks (minutes from midnight). */
    val windowStartMinute: Int = 8 * 60,
    /** Latest end for flexible tasks (minutes from midnight). */
    val windowEndMinute: Int = 21 * 60,

    val enabled: Boolean = true
) {
    val endMinute: Int get() = startMinute + durationMinutes

    fun hasWeekday(day: DayOfWeek): Boolean = (daysOfWeek and (1 shl (day.value - 1))) != 0

    companion object {
        /** Build a weekday bitmask from a set of DayOfWeek. */
        fun weekdayMask(days: Collection<DayOfWeek>): Int =
            days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

        fun weekdaysFromMask(mask: Int): List<DayOfWeek> =
            DayOfWeek.entries.filter { (mask and (1 shl (it.value - 1))) != 0 }
    }
}
