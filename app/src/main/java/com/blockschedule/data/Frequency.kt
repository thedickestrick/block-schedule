package com.blockschedule.data

/**
 * How often a task repeats.
 *
 * - ONCE: a single-occurrence task on [TaskEntity.anchorEpochDay] (e.g. a doctor appointment).
 * - DAILY/WEEKLY/BIWEEKLY/MONTHLY/YEARLY: recurring tasks scheduled automatically.
 *
 * For WEEKLY/BIWEEKLY the specific weekdays come from [TaskEntity.daysOfWeek].
 * For MONTHLY/YEARLY the day (and month) are derived from the anchor date.
 */
enum class Frequency(val label: String) {
    ONCE("One time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 weeks"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    val isRecurring: Boolean get() = this != ONCE

    companion object {
        fun fromName(name: String): Frequency =
            entries.firstOrNull { it.name == name } ?: ONCE
    }
}
