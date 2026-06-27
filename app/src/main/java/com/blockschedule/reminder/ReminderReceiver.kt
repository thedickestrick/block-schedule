package com.blockschedule.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.blockschedule.R
import com.blockschedule.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires for each scheduled block reminder, and re-schedules on the daily refresh / boot /
 * time changes.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMIND -> postReminder(context, intent)

            ReminderScheduler.ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ReminderScheduler.reschedule(context.applicationContext)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun postReminder(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)

        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Upcoming task"
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME) ?: ""
        val category = intent.getStringExtra(ReminderScheduler.EXTRA_CATEGORY) ?: ""
        val notifId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIF_ID, 1000)

        val open = PendingIntent.getActivity(
            context, notifId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val sub = listOf(time, category).filter { it.isNotBlank() }.joinToString("  ·  ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("Coming up: $title")
            .setContentText(sub.ifBlank { "Starting soon" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(notifId, notification) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Reminders shortly before each scheduled block." }
                )
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "reminders"
    }
}
