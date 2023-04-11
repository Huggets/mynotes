package com.huggets.mynotes.data

import androidx.room.TypeConverter

class Converter {
    @TypeConverter
    fun dateToString(date: Date) = date.toString()

    @TypeConverter
    fun stringToDate(string: String) = Date.fromString(string)
}