package com.blockschedule.schedule

import com.blockschedule.data.Frequency
import com.blockschedule.data.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure scheduling logic. Given the user's task definitions and a date, it works out
 * exactly which blocks land on that day:
 *
 *  1. Decide which tasks "occur" on the date (recurrence rules).
 *  2. Lay fixed-time tasks on the 0..1440 minute timeline, handling blocks that
 *     cross midnight (a block from yesterday can continue into today, and a block
 *     today can continue into tomorrow).
 *  3. Greedily fit flexible tasks into the gaps left inside their preferred window.
 *  4. Flag overlaps between fixed blocks so the UI can warn.
 *
 * Everything here is deterministic and side-effect free so it's easy to test and
 * to reuse from both the app UI and the home-screen widget.
 */
object Scheduler {

    private const val DAY = 24 * 60 // 1440

    /** Whether a task occurs on [date] per its recurrence rule. */
    fun occursOn(task: TaskEntity, date: LocalDate): Boolean {
        val anchor = LocalDate.ofEpochDay(task.anchorEpochDay)
        return when (task.frequency) {
            Frequency.ONCE -> date == anchor
            Frequency.DAILY -> !date.isBefore(anchor)
            Frequency.WEEKLY -> !date.isBefore(anchor) && matchesWeekday(task, date)
            Frequency.BIWEEKLY ->
                !date.isBefore(anchor) && matchesWeekday(task, date) && isEvenWeek(anchor, date)
            Frequency.MONTHLY -> !date.isBefore(anchor) && matchesMonthDay(anchor, date)
            Frequency.YEARLY ->
                !date.isBefore(anchor) && date.monthValue == anchor.monthValue && matchesMonthDay(anchor, date)
            Frequency.TIMES_PER_WEEK ->
                !date.isBefore(anchor) && date.dayOfWeek in spreadDays(task.count)
            Frequency.TIMES_PER_DAY -> !date.isBefore(anchor)
        }
    }

    /**
     * Pick [n] weekdays (Mon..Sun) spread as evenly as possible across the week, so
     * "3 times a week" lands on Mon/Wed/Fri, "2" on Mon/Thu, etc. Deterministic.
     */
    fun spreadDays(n: Int): Set<DayOfWeek> {
        val count = n.coerceIn(1, 7)
        return (0 until count).map { i -> DayOfWeek.of((i * 7 / count) + 1) }.toSet()
    }

    private fun matchesWeekday(task: TaskEntity, date: LocalDate): Boolean {
        // If no weekdays were chosen, fall back to the anchor's weekday.
        if (task.daysOfWeek == 0) {
            return date.dayOfWeek == LocalDate.ofEpochDay(task.anchorEpochDay).dayOfWeek
        }
        return task.hasWeekday(date.dayOfWeek)
    }

    private fun isEvenWeek(anchor: LocalDate, date: LocalDate): Boolean {
        val anchorMonday = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
        val dateMonday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val weeks = (dateMonday.toEpochDay() - anchorMonday.toEpochDay()) / 7
        return weeks % 2 == 0L
    }

    /** Matches the anchor's day-of-month, clamping to the last day of short months. */
    private fun matchesMonthDay(anchor: LocalDate, date: LocalDate): Boolean {
        val target = minOf(anchor.dayOfMonth, date.lengthOfMonth())
        return date.dayOfMonth == target
    }

    /**
     * Build the ordered list of blocks for [date] from all (enabled) [tasks].
     */
    fun blocksFor(date: LocalDate, tasks: List<TaskEntity>): List<ScheduledBlock> {
        val active = tasks.filter { it.enabled }
        // Top-level tasks vs sub-blocks (handled after their parents are placed).
        val topLevel = active.filter { it.parentId == null }
        val subBlocks = active.filter { it.parentId != null }

        val blocks = mutableListOf<ScheduledBlock>()
        // Occupied intervals on today's timeline, used for flexible placement.
        val occupied = mutableListOf<IntRange>()

        // 1) Fixed, single-time tasks that start today.
        topLevel.filter {
            it.isFixedTime && it.frequency != Frequency.TIMES_PER_DAY && occursOn(it, date)
        }.forEach { task ->
            val start = task.startMinute.coerceIn(0, DAY)
            val rawEnd = task.startMinute + task.durationMinutes
            val end = rawEnd.coerceAtMost(DAY)
            if (end > start) {
                blocks += ScheduledBlock(
                    taskId = task.id, title = task.title, category = task.category,
                    startMinute = start, endMinute = end, isFlexible = false,
                    continuesTomorrow = rawEnd > DAY
                )
                occupied += start until end
            }
        }

        // 2) Fixed tasks from yesterday that spill across midnight into today.
        val yesterday = date.minusDays(1)
        topLevel.filter {
            it.isFixedTime && it.frequency != Frequency.TIMES_PER_DAY && occursOn(it, yesterday)
        }.forEach { task ->
            val rawEnd = task.startMinute + task.durationMinutes
            if (rawEnd > DAY) {
                val end = (rawEnd - DAY).coerceAtMost(DAY)
                if (end > 0) {
                    blocks += ScheduledBlock(
                        taskId = task.id, title = task.title, category = task.category,
                        startMinute = 0, endMinute = end, isFlexible = false,
                        continuedFromYesterday = true
                    )
                    occupied += 0 until end
                }
            }
        }

        // 3) Flexible single-instance tasks: greedily fit into open gaps within their window.
        topLevel
            .filter { !it.isFixedTime && it.frequency != Frequency.TIMES_PER_DAY && occursOn(it, date) }
            .sortedWith(compareBy({ it.windowStartMinute }, { -it.durationMinutes }, { it.id }))
            .forEach { task ->
                placeFlexible(task, task.windowStartMinute, occupied, blocks)
            }

        // 4) "X times a day" tasks: place N instances spread across the window.
        topLevel.filter { it.frequency == Frequency.TIMES_PER_DAY && occursOn(it, date) }.forEach { task ->
            val n = task.count.coerceAtLeast(1)
            val ws = task.windowStartMinute.coerceIn(0, DAY)
            val we = task.windowEndMinute.coerceIn(0, DAY)
            val latestStart = (we - task.durationMinutes).coerceAtLeast(ws)
            for (i in 0 until n) {
                val target = if (n == 1) ws else ws + (latestStart - ws) * i / (n - 1)
                placeFlexible(task, target, occupied, blocks)
            }
        }

        // 5) Attach sub-blocks as children of their parent's block for today.
        val childrenByParent = mutableMapOf<Long, MutableList<ScheduledBlock>>()
        subBlocks.forEach { sub ->
            val parentTask = active.firstOrNull { it.id == sub.parentId } ?: return@forEach
            if (!occursOn(parentTask, date)) return@forEach
            val start = sub.startMinute.coerceIn(0, DAY)
            val end = (sub.startMinute + sub.durationMinutes).coerceAtMost(DAY)
            if (end > start) {
                childrenByParent.getOrPut(sub.parentId!!) { mutableListOf() } += ScheduledBlock(
                    taskId = sub.id, title = sub.title, category = sub.category,
                    startMinute = start, endMinute = end, isFlexible = false, isSubBlock = true
                )
            }
        }

        // 6) Flag overlaps among top-level placed blocks (sub-blocks are expected to nest).
        val placed = blocks.filter { !it.unscheduled }
        val withMeta = placed.map { b ->
            val conflict = placed.any { o ->
                o !== b && o.taskId != b.taskId &&
                    b.startMinute < o.endMinute && o.startMinute < b.endMinute
            }
            val kids = childrenByParent[b.taskId]?.sortedBy { it.startMinute } ?: emptyList()
            b.copy(hasConflict = conflict, children = kids)
        }

        val unscheduled = blocks.filter { it.unscheduled }
        return (withMeta.sortedBy { it.startMinute } + unscheduled)
    }

    /** Place one flexible instance starting at/after [preferredStart], else anywhere in window. */
    private fun placeFlexible(
        task: TaskEntity,
        preferredStart: Int,
        occupied: MutableList<IntRange>,
        blocks: MutableList<ScheduledBlock>
    ) {
        val ws = task.windowStartMinute.coerceIn(0, DAY)
        val we = task.windowEndMinute.coerceIn(0, DAY)
        val slot = findFreeSlot(occupied, preferredStart.coerceIn(ws, we), we, task.durationMinutes)
            ?: findFreeSlot(occupied, ws, we, task.durationMinutes)
        if (slot != null) {
            blocks += ScheduledBlock(
                taskId = task.id, title = task.title, category = task.category,
                startMinute = slot, endMinute = slot + task.durationMinutes, isFlexible = true
            )
            occupied += slot until (slot + task.durationMinutes)
        } else {
            blocks += ScheduledBlock(
                taskId = task.id, title = task.title, category = task.category,
                startMinute = -1, endMinute = -1, isFlexible = true, unscheduled = true
            )
        }
    }

    /**
     * Find the earliest start in [windowStart, windowEnd] where a [duration]-minute
     * block fits without overlapping anything in [occupied]. Returns null if none.
     */
    private fun findFreeSlot(
        occupied: List<IntRange>,
        windowStart: Int,
        windowEnd: Int,
        duration: Int
    ): Int? {
        if (duration <= 0 || windowEnd - windowStart < duration) return null
        val sorted = occupied.sortedBy { it.first }
        var cursor = windowStart
        for (range in sorted) {
            if (range.last + 1 <= cursor) continue          // already past this block
            if (range.first - cursor >= duration) return cursor  // fits before this block
            cursor = maxOf(cursor, range.last + 1)          // jump past the block
            if (cursor + duration > windowEnd) return null
        }
        return if (cursor + duration <= windowEnd) cursor else null
    }

    /** Index of the block containing [minuteOfDay], or -1. */
    fun currentBlockIndex(blocks: List<ScheduledBlock>, minuteOfDay: Int): Int =
        blocks.indexOfFirst { it.contains(minuteOfDay) }

    /**
     * The deepest block active at [minuteOfDay] — a sub-block takes precedence over its
     * parent (during lunch, "now" is Lunch, not Work). Returns null if nothing is active.
     */
    fun activeLeaf(blocks: List<ScheduledBlock>, minuteOfDay: Int): ScheduledBlock? {
        val parent = blocks.firstOrNull { b ->
            b.contains(minuteOfDay) || b.children.any { it.contains(minuteOfDay) }
        } ?: return null
        return parent.children.firstOrNull { it.contains(minuteOfDay) } ?: parent
    }

    /** The key of the active leaf (see [activeLeaf]), or null. */
    fun activeLeafKey(blocks: List<ScheduledBlock>, minuteOfDay: Int): String? =
        activeLeaf(blocks, minuteOfDay)?.key

    /** Flattens top-level blocks and their children into a single list (parents before kids). */
    fun flatten(blocks: List<ScheduledBlock>): List<ScheduledBlock> =
        blocks.flatMap { listOf(it) + it.children }
}
