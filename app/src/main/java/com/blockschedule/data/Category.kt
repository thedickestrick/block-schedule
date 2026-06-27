package com.blockschedule.data

/**
 * Task categories. Each carries a distinct, high-contrast color so blocks are
 * easy to tell apart at a glance on the timeline and the home-screen widget.
 * Colors are stored as ARGB longs (0xAARRGGBB).
 */
enum class Category(val label: String, val colorArgb: Long) {
    WORK("Work", 0xFF3B5BDB),
    SLEEP("Sleep", 0xFF5F3DC4),
    GYM("Gym / Exercise", 0xFF2B8A3E),
    MEAL("Meal", 0xFFE8590C),
    APPOINTMENT("Appointment", 0xFFC2255C),
    MEDICATION("Medication", 0xFF1098AD),
    SELF_CARE("Self-care", 0xFF9C36B5),
    CHORE("Chore", 0xFF845EF7),
    SOCIAL("Social", 0xFF0CA678),
    OTHER("Other", 0xFF495057);

    companion object {
        fun fromName(name: String): Category =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
