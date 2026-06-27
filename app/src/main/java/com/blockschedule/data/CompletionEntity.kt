package com.blockschedule.data

import androidx.room.Entity

/**
 * How many instances of a task have been completed on a given day.
 * For most tasks this is 0 or 1; for "X times a day" tasks it can be 0..N.
 * Keyed by task + day so completion is tracked per occurrence, not on the task itself.
 */
@Entity(tableName = "completions", primaryKeys = ["taskId", "epochDay"])
data class CompletionEntity(
    val taskId: Long,
    val epochDay: Long,
    val doneCount: Int
)
