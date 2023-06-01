package com.huggets.mynotes.data

import java.util.Calendar

/**
 * Represents a date with year, month, day, hour, minute, second and millisecond.
 *
 * @property year The year of the date.
 * @property month The month of the date.
 * @property day The day of the date.
 * @property hour The hour of the date.
 * @property minute The minute of the date.
 * @property second The second of the date.
 * @property millisecond The millisecond of the date.
 */
data class Date(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val millisecond: Int,
) : Comparable<Date> {

    /**
     * Returns a string representation of the number with at least two digits.
     */
    private fun toTwoDigitString(number: Int): String {
        return if (number < 10) "0$number" else number.toString()
    }

    /**
     * Returns a string representation of the number with at least three digits.
     */
    private fun toThreeDigitString(number: Int): String {
        return if (number < 10) "00$number"
        else if (number < 100) "0$number"
        else number.toString()
    }

    /**
     * Returns a string representation of the number with at least four digits.
     */
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

    override fun compareTo(other: Date): Int {
        if (this.year != other.year) return this.year - other.year
        else if (this.month != other.month) return this.month - other.month
        else if (this.day != other.day) return this.day - other.day
        else if (this.hour != other.hour) return this.hour - other.hour
        else if (this.minute != other.minute) return this.minute - other.minute
        else if (this.second != other.second) return this.second - other.second
        else if (this.millisecond != other.millisecond) return this.millisecond - other.millisecond

        return 0
    }

    companion object {

        /**
         * Returns the current time as a [Date] object.
         */
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

        /**
         * Returns a [Date] object from a string representation.
         */
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
