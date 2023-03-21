package com.huggets.mynotes.note

data class Note(
    var id: Int,
    var title: String,
    var content: String
)

fun List<Note>.find(noteId: Int): Note? {
    forEach {
        if (it.id == noteId) return it
    }

    return null
}