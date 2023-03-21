package com.huggets.mynotes.note

data class NoteItemUiState(
    val id: Int,
    val title: String,
    val content: String
) {
    fun toNote() = Note(id, title, content)
}

fun List<NoteItemUiState>.find(noteId: Int): NoteItemUiState? {
    forEach {
        if (it.id == noteId) return it
    }

    return null
}