package com.blockschedule.update

import android.content.Context

/** Tiny SharedPreferences wrapper for the auto-update toggle. */
class UpdatePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var autoCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO, value).apply()

    companion object {
        private const val KEY_AUTO = "auto_check"
    }
}
