package com.blockschedule.game

import android.content.Context
import com.blockschedule.data.TaskRepository
import com.blockschedule.schedule.Scheduler
import com.blockschedule.widget.WidgetUpdater
import java.time.LocalDate

/** Result of toggling a block's completion, used to drive feedback (confetti, +points). */
data class ToggleResult(
    val nowDone: Boolean,
    val pointsDelta: Int,
    val dayJustCompleted: Boolean
)

/**
 * Single source of truth for completing/uncompleting a block instance. Shared by the in-app
 * UI and the home-screen widget so points, streak, and the widget stay consistent.
 */
object CompletionManager {

    suspend fun toggle(
        context: Context,
        taskId: Long,
        instanceIndex: Int,
        epochDay: Long
    ): ToggleResult {
        val repo = TaskRepository.from(context)
        val game = GamePrefs(context)

        val current = repo.getCompletions(epochDay).firstOrNull { it.taskId == taskId }?.doneCount ?: 0
        val wasDone = instanceIndex < current
        val newCount = if (wasDone) current - 1 else current + 1
        repo.setDoneCount(taskId, epochDay, newCount)

        var delta = 0
        if (!wasDone) {
            delta += GamePrefs.POINTS_PER_TASK
            if (epochDay == LocalDate.now().toEpochDay()) game.bumpStreakForToday(epochDay)
        } else {
            delta -= GamePrefs.POINTS_PER_TASK
        }

        // Did this complete the whole day?
        val tasks = repo.enabledTasks()
        val blocks = Scheduler.blocksFor(LocalDate.ofEpochDay(epochDay), tasks)
        val counts = repo.getCompletions(epochDay).associate { it.taskId to it.doneCount }
        val (done, total) = Scheduler.progress(blocks, counts)
        val dayJustCompleted = !wasDone && total > 0 && done == total
        if (dayJustCompleted) delta += GamePrefs.POINTS_DAY_BONUS

        game.addPoints(delta)
        WidgetUpdater.update(context)

        return ToggleResult(nowDone = !wasDone, pointsDelta = delta, dayJustCompleted = dayJustCompleted)
    }
}
