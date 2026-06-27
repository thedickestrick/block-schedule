package com.blockschedule.game

/** A form of the streak buddy — it evolves into cuter/cooler animals as the streak grows. */
data class BuddyForm(val minStreak: Int, val emoji: String, val name: String)

/**
 * The companion that grows with the daily streak. Each milestone evolves it into a new
 * animal — something to nurture and not want to lose.
 */
object Buddy {
    val FORMS = listOf(
        BuddyForm(0, "🥚", "Mystery Egg"),
        BuddyForm(1, "🐣", "Lil' Hatchling"),
        BuddyForm(3, "🐰", "Hoppy Bun"),
        BuddyForm(7, "🦊", "Clever Fox"),
        BuddyForm(14, "🐯", "Brave Tiger"),
        BuddyForm(30, "🦁", "Lion Heart"),
        BuddyForm(60, "🦄", "Magic Unicorn"),
        BuddyForm(100, "🐉", "Mighty Dragon")
    )

    fun formFor(streak: Int): BuddyForm = FORMS.last { streak >= it.minStreak }

    /** The next form to grow into, or null if fully evolved. */
    fun nextForm(streak: Int): BuddyForm? = FORMS.firstOrNull { it.minStreak > streak }

    /** True if [newStreak] crossed into a new form versus [oldStreak]. */
    fun evolvedForm(oldStreak: Int, newStreak: Int): BuddyForm? {
        val before = formFor(oldStreak)
        val after = formFor(newStreak)
        return if (after.name != before.name) after else null
    }
}
