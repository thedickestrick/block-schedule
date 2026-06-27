package com.blockschedule.reminder

import android.content.Context

/** Settings for block reminders: on/off and how many minutes ahead to notify. */
class ReminderPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var leadMinutes: Int
        get() = prefs.getInt(KEY_LEAD, 10)
        set(value) = prefs.edit().putInt(KEY_LEAD, value).apply()

    /** Request codes of alarms we've scheduled, so we can cancel them on reschedule. */
    var scheduledCodes: Set<Int>
        get() = prefs.getStringSet(KEY_CODES, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
        set(value) = prefs.edit().putStringSet(KEY_CODES, value.map { it.toString() }.toSet()).apply()

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LEAD = "lead_minutes"
        private const val KEY_CODES = "scheduled_codes"
    }
}
