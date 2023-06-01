package com.huggets.mynotes.data

import androidx.room.TypeConverter

/**
 * Converter for the [ApplicationDatabase].
 */
class Converter {
    /**
     * Convert a [String] to a [Date].
     */
    @TypeConverter
    fun dateToString(date: Date) = date.toString()

    /**
     * Convert a [Date] to a [String].
     */
    @TypeConverter
    fun stringToDate(string: String) = Date.fromString(string)
}