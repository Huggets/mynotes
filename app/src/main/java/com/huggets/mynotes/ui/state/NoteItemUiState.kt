package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note

// TODO Look if dates are needed
data class NoteItemUiState(
    val id: Int,
    val title: String,
    val content: String,
    val creationDate: Date,
    val lastEditTime: Date,
) {
    constructor(note: Note) : this(
        note.id,
        note.title,
        note.content,
        note.creationDate,
        note.lastEditTime
    )

    fun toNote() = Note(id, title, content, creationDate, lastEditTime)
}

fun List<NoteItemUiState>.find(id: Int): NoteItemUiState? {
    for (note in this) {
        if (note.id == id) return note
    }

    return null
}