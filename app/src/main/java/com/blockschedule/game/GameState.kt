package com.blockschedule.game

/** Snapshot of the player's progress, shown in the header and achievements screen. */
data class GameState(
    val points: Int,
    val level: Int,
    val levelProgress: Int,
    val pointsPerLevel: Int,
    val streak: Int,
    val bestStreak: Int,
    val totalCompleted: Int = 0,
    val perfectDays: Int = 0,
    val unlocked: Set<String> = emptySet()
)

/** One-off celebratory events the UI reacts to (animals, sound, floating points). */
sealed interface GameEvent {
    data class PointsEarned(val delta: Int) : GameEvent
    data object DayCompleted : GameEvent
    data class AchievementUnlocked(val achievement: Achievement) : GameEvent
}
