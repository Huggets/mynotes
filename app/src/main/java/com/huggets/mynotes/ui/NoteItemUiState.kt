package com.huggets.mynotes.ui

import com.huggets.mynotes.note.Note
import java.util.*

data class NoteItemUiState(
    val id: Long,
    val title: String,
    val content: String,
    var lastEditTime: String,
) {
    constructor(note: Note) : this(note.id, note.title, note.content, note.lastEditTime)

    fun toNote() = Note(id, title, content, lastEditTime)

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

            return stringBuilder.toString()
        }
    }
}

fun List<NoteItemUiState>.find(noteId: Long): NoteItemUiState? {
    for (note in this) {
        if (note.id == noteId) return note
    }

    return null
}