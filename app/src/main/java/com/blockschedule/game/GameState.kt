package com.blockschedule.game

/** Snapshot of the player's progress, shown in the header. */
data class GameState(
    val points: Int,
    val level: Int,
    val levelProgress: Int,
    val pointsPerLevel: Int,
    val streak: Int,
    val bestStreak: Int
)

/** One-off celebratory events the UI reacts to (haptics, confetti, floating points). */
sealed interface GameEvent {
    data class PointsEarned(val delta: Int) : GameEvent
    data object DayCompleted : GameEvent
}
