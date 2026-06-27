package com.blockschedule.game

import android.content.Context

/**
 * Persistent "game" state: points, level, and a gentle daily streak.
 * Points never go below zero; the streak only ever grows or resets — it's never
 * punished for un-checking something (kept low-pressure on purpose).
 */
class GamePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)

    var points: Int
        get() = prefs.getInt(KEY_POINTS, 0)
        private set(value) = prefs.edit().putInt(KEY_POINTS, value.coerceAtLeast(0)).apply()

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        private set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    var bestStreak: Int
        get() = prefs.getInt(KEY_BEST, 0)
        private set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    private var lastDay: Long
        get() = prefs.getLong(KEY_LAST_DAY, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_DAY, value).apply()

    /** Level grows every [POINTS_PER_LEVEL] points, starting at 1. */
    val level: Int get() = 1 + points / POINTS_PER_LEVEL
    /** Points into the current level, and points needed for the level. */
    val levelProgress: Int get() = points % POINTS_PER_LEVEL
    val pointsPerLevel: Int get() = POINTS_PER_LEVEL

    fun addPoints(delta: Int) { points += delta }

    /** Count today toward the streak the first time something is completed today. */
    fun bumpStreakForToday(today: Long) {
        if (lastDay == today) return
        streak = if (lastDay == today - 1) streak + 1 else 1
        lastDay = today
        if (streak > bestStreak) bestStreak = streak
    }

    companion object {
        const val POINTS_PER_LEVEL = 100
        const val POINTS_PER_TASK = 10
        const val POINTS_DAY_BONUS = 50
        private const val KEY_POINTS = "points"
        private const val KEY_STREAK = "streak"
        private const val KEY_BEST = "best_streak"
        private const val KEY_LAST_DAY = "last_day"
    }
}
