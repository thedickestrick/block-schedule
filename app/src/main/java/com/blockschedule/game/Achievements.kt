package com.blockschedule.game

/** A cute, animal-themed badge. */
data class Achievement(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val bonus: Int = 25
)

/** The numbers an achievement is evaluated against. */
data class GameStats(
    val totalCompleted: Int,
    val bestStreak: Int,
    val perfectDays: Int,
    val level: Int,
    val completionHour: Int
)

object Achievements {

    val ALL: List<Achievement> = listOf(
        Achievement("first", "First Step", "🐣", "Finish your very first task"),
        Achievement("tasks10", "Getting Cozy", "🐰", "Finish 10 tasks"),
        Achievement("tasks50", "On a Roll", "🦊", "Finish 50 tasks"),
        Achievement("tasks100", "Superstar Kitty", "🐱", "Finish 100 tasks"),
        Achievement("tasks250", "Unicorn Status", "🦄", "Finish 250 tasks"),
        Achievement("streak3", "Good Pup", "🐶", "Keep a 3-day streak"),
        Achievement("streak7", "Bear Hug", "🐻", "Keep a 7-day streak"),
        Achievement("streak30", "Lion Heart", "🦁", "Keep a 30-day streak", bonus = 100),
        Achievement("perfect", "Perfect Day", "🐥", "Finish everything in a day"),
        Achievement("perfect7", "Panda-monium", "🐼", "Have 7 perfect days", bonus = 75),
        Achievement("early", "Early Bird", "🐤", "Finish a task before 7am"),
        Achievement("night", "Night Owl", "🦉", "Finish a task after 10pm")
    )

    fun byId(id: String): Achievement? = ALL.firstOrNull { it.id == id }

    /** True if the badge's condition is currently met. */
    fun isEarned(id: String, s: GameStats): Boolean = when (id) {
        "first" -> s.totalCompleted >= 1
        "tasks10" -> s.totalCompleted >= 10
        "tasks50" -> s.totalCompleted >= 50
        "tasks100" -> s.totalCompleted >= 100
        "tasks250" -> s.totalCompleted >= 250
        "streak3" -> s.bestStreak >= 3
        "streak7" -> s.bestStreak >= 7
        "streak30" -> s.bestStreak >= 30
        "perfect" -> s.perfectDays >= 1
        "perfect7" -> s.perfectDays >= 7
        "early" -> s.completionHour in 0..6
        "night" -> s.completionHour >= 22
        else -> false
    }

    /** Newly-earned badges not already in [alreadyUnlocked]. */
    fun newlyEarned(s: GameStats, alreadyUnlocked: Set<String>): List<Achievement> =
        ALL.filter { it.id !in alreadyUnlocked && isEarned(it.id, s) }
}
