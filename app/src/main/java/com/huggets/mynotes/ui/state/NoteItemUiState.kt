package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note

data class NoteItemUiState(
    val title: String,
    val content: String,
    val creationDate: Date,
    val lastEditTime: Date,
) {
    constructor(note: Note) : this(note.title, note.content, note.creationDate, note.lastEditTime)

    fun toNote() = Note(title, content, creationDate, lastEditTime)
}

fun List<NoteItemUiState>.find(creationDate: Date): NoteItemUiState? {
    for (note in this) {
        if (note.creationDate == creationDate) return note
    }

    return null
}