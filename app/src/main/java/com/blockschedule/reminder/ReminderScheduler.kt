package com.blockschedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blockschedule.data.TaskRepository
import com.blockschedule.schedule.ScheduledBlock
import com.blockschedule.schedule.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Schedules a notification a few minutes before each of today's blocks, plus a daily
 * "refresh" alarm that re-runs this at the start of the next day (so tomorrow's blocks get
 * their reminders too). Re-run whenever tasks change, on app launch, and on boot.
 */
object ReminderScheduler {

    const val ACTION_REMIND = "com.blockschedule.action.REMIND"
    const val ACTION_REFRESH = "com.blockschedule.action.REFRESH"

    const val EXTRA_TITLE = "title"
    const val EXTRA_TIME = "time"
    const val EXTRA_CATEGORY = "category"
    const val EXTRA_NOTIF_ID = "notif_id"

    private const val REFRESH_CODE = 1

    /** Fire-and-forget reschedule for non-coroutine callers (receivers use goAsync separately). */
    fun rescheduleAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch { reschedule(app) }
    }

    suspend fun reschedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = ReminderPrefs(context)

        // Cancel everything we scheduled last time.
        prefs.scheduledCodes.forEach { code ->
            am.cancel(remindPendingIntent(context, code, cancelOnly = true))
        }

        // Always keep a daily refresh alarm alive.
        scheduleDailyRefresh(context, am)

        val newCodes = mutableSetOf<Int>()
        if (prefs.enabled) {
            val tasks = TaskRepository.from(context).enabledTasks()
            val today = LocalDate.now()
            val blocks = Scheduler.blocksFor(today, tasks)
            val midnight = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val lead = prefs.leadMinutes

            blocks.filter { !it.unscheduled && !it.continuedFromYesterday }.forEach { b ->
                val triggerMillis = midnight + (b.startMinute - lead) * 60_000L
                if (triggerMillis > now) {
                    val code = blockCode(b)
                    setExact(am, triggerMillis, remindPendingIntent(context, code, block = b))
                    newCodes += code
                }
            }
        }
        prefs.scheduledCodes = newCodes
    }

    private fun blockCode(b: ScheduledBlock): Int =
        (((b.taskId % 100000) * 1500) + b.startMinute + 100).toInt()

    private fun remindPendingIntent(
        context: Context,
        code: Int,
        block: ScheduledBlock? = null,
        cancelOnly: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        if (!cancelOnly && block != null) {
            intent.putExtra(EXTRA_TITLE, block.title)
            intent.putExtra(EXTRA_TIME, ScheduledBlock.formatTime(block.startMinute))
            intent.putExtra(EXTRA_CATEGORY, block.category.label)
            intent.putExtra(EXTRA_NOTIF_ID, code)
        }
        return PendingIntent.getBroadcast(context, code, intent, piFlags())
    }

    private fun scheduleDailyRefresh(context: Context, am: AlarmManager) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REFRESH)
        val pi = PendingIntent.getBroadcast(context, REFRESH_CODE, intent, piFlags())
        val next = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(5)
            .toInstant().toEpochMilli()
        setExact(am, next, pi)
    }

    private fun setExact(am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun piFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
