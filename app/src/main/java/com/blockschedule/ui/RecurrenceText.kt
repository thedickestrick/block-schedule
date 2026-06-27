package com.blockschedule.ui

import com.blockschedule.data.Frequency
import com.blockschedule.data.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Builds short human-readable descriptions of a task's recurrence. */
object RecurrenceText {

    private fun weekdayList(task: TaskEntity): String {
        val days = if (task.daysOfWeek == 0) {
            listOf(LocalDate.ofEpochDay(task.anchorEpochDay).dayOfWeek)
        } else {
            TaskEntity.weekdaysFromMask(task.daysOfWeek)
        }
        return days.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    private fun ordinal(n: Int): String {
        val suffix = when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    fun summary(task: TaskEntity): String {
        val anchor = LocalDate.ofEpochDay(task.anchorEpochDay)
        return when (task.frequency) {
            Frequency.ONCE -> "Once on ${anchor.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
            Frequency.DAILY -> "Every day"
            Frequency.WEEKLY -> "Weekly · ${weekdayList(task)}"
            Frequency.BIWEEKLY -> "Every 2 weeks · ${weekdayList(task)}"
            Frequency.MONTHLY -> "Monthly · ${ordinal(anchor.dayOfMonth)}"
            Frequency.YEARLY -> "Yearly · ${anchor.format(DateTimeFormatter.ofPattern("MMM d"))}"
        }
    }
}
