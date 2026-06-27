package com.blockschedule.game

import android.content.Context
import com.blockschedule.data.TaskRepository
import com.blockschedule.schedule.Scheduler
import com.blockschedule.widget.WidgetUpdater
import java.time.LocalDate
import java.time.LocalTime

/** Result of toggling a block's completion, used to drive feedback (animals, sound, points). */
data class ToggleResult(
    val nowDone: Boolean,
    val pointsDelta: Int,
    val dayJustCompleted: Boolean,
    val taskParty: Boolean = false,
    val unlocked: List<Achievement> = emptyList()
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
            game.addCompleted()
            if (epochDay == LocalDate.now().toEpochDay()) game.bumpStreakForToday(epochDay)
        } else {
            delta -= GamePrefs.POINTS_PER_TASK
        }

        // Did this complete the whole day?
        val tasks = repo.enabledTasks()
        val taskParty = !wasDone && tasks.firstOrNull { it.id == taskId }?.partyOnComplete == true
        val blocks = Scheduler.blocksFor(LocalDate.ofEpochDay(epochDay), tasks)
        val counts = repo.getCompletions(epochDay).associate { it.taskId to it.doneCount }
        val (done, total) = Scheduler.progress(blocks, counts)
        val dayJustCompleted = !wasDone && total > 0 && done == total
        if (dayJustCompleted) {
            if (game.recordPerfectDay(epochDay)) delta += GamePrefs.POINTS_DAY_BONUS
        }

        game.addPoints(delta)

        // Evaluate achievements (only when completing, and if enabled).
        var unlocked = emptyList<Achievement>()
        if (!wasDone && game.achievementsEnabled) {
            val stats = GameStats(
                totalCompleted = game.totalCompleted,
                bestStreak = game.bestStreak,
                perfectDays = game.perfectDays,
                level = game.level,
                completionHour = LocalTime.now().hour
            )
            unlocked = Achievements.newlyEarned(stats, game.unlocked)
            unlocked.forEach {
                game.unlock(it.id)
                game.addPoints(it.bonus)
            }
        }

        WidgetUpdater.update(context)

        return ToggleResult(
            nowDone = !wasDone,
            pointsDelta = delta,
            dayJustCompleted = dayJustCompleted,
            taskParty = taskParty,
            unlocked = unlocked
        )
    }
}
