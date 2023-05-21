package com.huggets.mynotes.data

import java.util.Calendar

data class Date(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val millisecond: Int,
) {
    private fun toTwoDigitString(number: Int): String {
        return if (number < 10) "0$number" else number.toString()
    }

    private fun toThreeDigitString(number: Int): String {
        return if (number < 10) "00$number"
        else if (number < 100) "0$number"
        else number.toString()
    }

    private fun toFourDigitString(number: Int): String {
        return if (number < 10) "000$number"
        else if (number < 100) "00$number"
        else if (number < 1000) "0$number"
        else number.toString()
    }

    override fun toString(): String {
        val year = toFourDigitString(this.year)
        val month = toTwoDigitString(this.month)
        val day = toTwoDigitString(this.day)
        val hour = toTwoDigitString(this.hour)
        val minute = toTwoDigitString(this.minute)
        val second = toTwoDigitString(this.second)
        val millisecond = toThreeDigitString(this.millisecond)

        return "$year-$month-$day $hour:$minute:$second.$millisecond"
    }

    companion object {
        fun getCurrentTime(): Date {
            val calendar = Calendar.getInstance()

            return Date(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND),
            )
        }

        fun fromString(string: String): Date {
            val year = string.substring(0, 4).toInt()
            val month = string.substring(5, 7).toInt()
            val day = string.substring(8, 10).toInt()
            val hour = string.substring(11, 13).toInt()
            val minute = string.substring(14, 16).toInt()
            val second = string.substring(17, 19).toInt()
            val millisecond = string.substring(20, 23).toInt()

            return Date(year, month, day, hour, minute, second, millisecond)
        }
    }
}
