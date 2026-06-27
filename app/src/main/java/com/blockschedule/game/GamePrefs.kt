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

    // --- lifetime stats (for achievements) ---

    var totalCompleted: Int
        get() = prefs.getInt(KEY_TOTAL, 0)
        private set(value) = prefs.edit().putInt(KEY_TOTAL, value).apply()

    var perfectDays: Int
        get() = prefs.getInt(KEY_PERFECT, 0)
        private set(value) = prefs.edit().putInt(KEY_PERFECT, value).apply()

    private var lastPerfectDay: Long
        get() = prefs.getLong(KEY_LAST_PERFECT, -1)
        set(value) = prefs.edit().putLong(KEY_LAST_PERFECT, value).apply()

    var unlocked: Set<String>
        get() = prefs.getStringSet(KEY_UNLOCKED, emptySet())!!.toSet()
        private set(value) = prefs.edit().putStringSet(KEY_UNLOCKED, value).apply()

    // --- feature toggles ---

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var celebrationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CELEBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_CELEBRATE, value).apply()

    var achievementsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACHIEVE, true)
        set(value) = prefs.edit().putBoolean(KEY_ACHIEVE, value).apply()

    fun addCompleted() { totalCompleted += 1 }

    /** Records a perfect day; returns true if it's newly counted (not the same day twice). */
    fun recordPerfectDay(today: Long): Boolean {
        if (lastPerfectDay == today) return false
        lastPerfectDay = today
        perfectDays += 1
        return true
    }

    fun unlock(id: String) { unlocked = unlocked + id }

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
        private const val KEY_TOTAL = "total_completed"
        private const val KEY_PERFECT = "perfect_days"
        private const val KEY_LAST_PERFECT = "last_perfect_day"
        private const val KEY_UNLOCKED = "unlocked"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_CELEBRATE = "celebrations_enabled"
        private const val KEY_ACHIEVE = "achievements_enabled"
    }
}
