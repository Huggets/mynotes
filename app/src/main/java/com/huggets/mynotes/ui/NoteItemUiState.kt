package com.huggets.mynotes.ui

import com.huggets.mynotes.note.Note

data class NoteItemUiState(
    val id: Long,
    val title: String,
    val content: String
) {
    fun toNote() = Note(id, title, content)
}

fun List<NoteItemUiState>.find(noteId: Long): NoteItemUiState? {
    for (note in this) {
        if (note.id == noteId) return note
    }

    return null
}