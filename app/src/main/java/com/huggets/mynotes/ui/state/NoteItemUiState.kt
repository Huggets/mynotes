package com.huggets.mynotes.ui.state

import com.huggets.mynotes.note.Note
import java.util.*

data class NoteItemUiState(
    val title: String,
    val content: String,
    val creationDate: String,
    val lastEditTime: String,
) {
    constructor(note: Note) : this(note.title, note.content, note.creationDate, note.lastEditTime)

    fun toNote() = Note(title, content, creationDate, lastEditTime)

    companion object {
        fun getCurrentEditTime(): String {
            val stringBuilder = StringBuilder()

            val calendar = Calendar.getInstance()
            stringBuilder.append(calendar.get(Calendar.YEAR))
            stringBuilder.append("-")
            stringBuilder.append(calendar.get(Calendar.MONTH))
            stringBuilder.append("-")
            stringBuilder.append(calendar.get(Calendar.DAY_OF_MONTH))
            stringBuilder.append(" ")
            stringBuilder.append(calendar.get(Calendar.HOUR_OF_DAY))
            stringBuilder.append(":")
            stringBuilder.append(calendar.get(Calendar.MINUTE))
            stringBuilder.append(":")
            stringBuilder.append(calendar.get(Calendar.SECOND))
            stringBuilder.append(".")
            stringBuilder.append(calendar.get(Calendar.MILLISECOND))

            return stringBuilder.toString()
        }
    }
}

fun List<NoteItemUiState>.find(creationDate: String): NoteItemUiState? {
    for (note in this) {
        if (note.creationDate == creationDate) return note
    }

    return null
}