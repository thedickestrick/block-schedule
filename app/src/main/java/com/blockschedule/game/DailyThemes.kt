package com.blockschedule.game

import java.time.LocalDate

/** A cute theme: a name, a headline emoji, and the cast of animals who show up that day. */
data class DailyTheme(val name: String, val emoji: String, val animals: List<String>)

/**
 * Picks a different cute theme for every day so the app feels fresh daily. The base theme
 * rotates each day (so two days running never match) and shifts year to year, holidays get
 * their own special theme, and a per-day "featured friend" keeps each calendar day distinct.
 */
object DailyThemes {

    private val THEMES = listOf(
        DailyTheme("Kitty Cuddles", "🐱", listOf("🐱", "😺", "😸", "😻", "🐈")),
        DailyTheme("Puppy Party", "🐶", listOf("🐶", "🐕", "🦮", "🐩")),
        DailyTheme("Forest Friends", "🦊", listOf("🦊", "🦝", "🦔", "🦉", "🐿️", "🐻")),
        DailyTheme("Farm Animals", "🐮", listOf("🐮", "🐷", "🐔", "🐴", "🐑", "🐐", "🦆")),
        DailyTheme("Ocean Pals", "🐙", listOf("🐙", "🐠", "🐟", "🐬", "🐳", "🦀")),
        DailyTheme("Bunny Hop", "🐰", listOf("🐰", "🐇", "🥕", "🌸")),
        DailyTheme("Safari Day", "🦁", listOf("🦁", "🐯", "🐘", "🦒", "🦓")),
        DailyTheme("Polar Pals", "🐧", listOf("🐧", "🐻‍❄️", "🦭", "❄️")),
        DailyTheme("Bug Buddies", "🐝", listOf("🐝", "🐞", "🦋", "🐛", "🐌")),
        DailyTheme("Panda Picnic", "🐼", listOf("🐼", "🎋", "🐾")),
        DailyTheme("Jungle Jamboree", "🐵", listOf("🐵", "🦍", "🦧", "🦜", "🐸")),
        DailyTheme("Unicorn Dreams", "🦄", listOf("🦄", "🌈", "⭐", "✨")),
        DailyTheme("Dino Day", "🦕", listOf("🦕", "🦖", "🐊")),
        DailyTheme("Birdie Brunch", "🐦", listOf("🐦", "🐤", "🦜", "🦢", "🦩")),
        DailyTheme("Mouse House", "🐭", listOf("🐭", "🐹", "🧀")),
        DailyTheme("Frog Pond", "🐸", listOf("🐸", "🦎", "🐢", "🪷")),
        DailyTheme("Bear Hugs", "🐻", listOf("🐻", "🐨", "🧸", "🍯")),
        DailyTheme("Hatchlings", "🐣", listOf("🐣", "🐤", "🐥", "🥚")),
        DailyTheme("Koala Cuddle", "🐨", listOf("🐨", "🌿", "💚")),
        DailyTheme("Hammy Time", "🐹", listOf("🐹", "🌻", "🥜")),
        DailyTheme("Whale Hello", "🐳", listOf("🐳", "🐋", "💦", "🌊")),
        DailyTheme("Butterfly Garden", "🦋", listOf("🦋", "🌸", "🌷", "🌼")),
        DailyTheme("Piggy Wiggly", "🐷", listOf("🐷", "🐽", "💕")),
        DailyTheme("Owl Always Love You", "🦉", listOf("🦉", "🌙", "⭐")),
        DailyTheme("Crabby Beach", "🦀", listOf("🦀", "🦞", "🏖️", "🐚")),
        DailyTheme("Hedgie Hugs", "🦔", listOf("🦔", "🍄", "🍂")),
        DailyTheme("Llama Drama", "🦙", listOf("🦙", "🌵", "⭐")),
        DailyTheme("Turtley Awesome", "🐢", listOf("🐢", "🌊", "🪸")),
        DailyTheme("Otterly Cute", "🦦", listOf("🦦", "🐚", "💦")),
        DailyTheme("Sloth Mode", "🦥", listOf("🦥", "🌴", "😴")),
        DailyTheme("Flamingo Fun", "🦩", listOf("🦩", "🌺", "🌴")),
        DailyTheme("Honeybee", "🐝", listOf("🐝", "🌼", "🍯")),
        DailyTheme("Deer Friend", "🦌", listOf("🦌", "🌿", "🍃")),
        DailyTheme("Ducky Day", "🦆", listOf("🦆", "🐤", "💧")),
        DailyTheme("Sheepish", "🐑", listOf("🐑", "🐐", "☁️")),
        DailyTheme("Reindeer Run", "🦌", listOf("🦌", "🌲", "❄️")),
        DailyTheme("Chick Magnet", "🐥", listOf("🐥", "🐤", "🌻")),
        DailyTheme("Raccoon Raid", "🦝", listOf("🦝", "🌙", "⭐")),
        DailyTheme("Lizard Lounge", "🦎", listOf("🦎", "🌵", "☀️")),
        DailyTheme("Snail Mail", "🐌", listOf("🐌", "🍃", "🌧️"))
    )

    private val HOLIDAYS = mapOf(
        "1-1" to DailyTheme("Happy New Year!", "🎉", listOf("🎉", "🥳", "🎊", "🦄", "✨")),
        "2-14" to DailyTheme("Valentine's Day", "💕", listOf("💕", "🐰", "🐱", "🌹", "💝")),
        "3-17" to DailyTheme("Lucky Day", "🍀", listOf("🍀", "🌈", "🦄", "✨")),
        "4-1" to DailyTheme("Silly Day", "🤪", listOf("🐵", "🤪", "🃏", "🐸")),
        "10-31" to DailyTheme("Spooky Cute", "🎃", listOf("🎃", "👻", "🦇", "🐈‍⬛", "🕷️")),
        "12-24" to DailyTheme("Christmas Eve", "🎄", listOf("🎄", "🦌", "⛄", "🎁", "✨")),
        "12-25" to DailyTheme("Merry Christmas!", "🎅", listOf("🎅", "🦌", "🎄", "⛄", "🎁")),
        "12-31" to DailyTheme("New Year's Eve", "🎆", listOf("🎆", "🥳", "🍾", "✨"))
    )

    fun forDate(date: LocalDate): DailyTheme {
        HOLIDAYS["${date.monthValue}-${date.dayOfMonth}"]?.let { return it }
        val idx = Math.floorMod(date.toEpochDay(), THEMES.size.toLong()).toInt()
        return THEMES[idx]
    }

    /** A featured friend for the day, drawn from the day's theme. */
    fun featuredAnimal(date: LocalDate): String {
        val theme = forDate(date)
        return theme.animals[date.dayOfYear % theme.animals.size]
    }
}
