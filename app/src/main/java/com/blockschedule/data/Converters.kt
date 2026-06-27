package com.blockschedule.data

import androidx.room.TypeConverter

/** Room type converters for the enum columns (stored as their stable enum name). */
class Converters {
    @TypeConverter
    fun categoryToString(c: Category): String = c.name

    @TypeConverter
    fun stringToCategory(s: String): Category = Category.fromName(s)

    @TypeConverter
    fun frequencyToString(f: Frequency): String = f.name

    @TypeConverter
    fun stringToFrequency(s: String): Frequency = Frequency.fromName(s)
}
