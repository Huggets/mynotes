package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note

/**
 * Represents the state of a note.
 *
 * @property title The title of the note.
 * @property content The content of the note.
 * @property creationDate The creation date of the note.
 * @property lastEditTime The last modification date of the note.
 */
data class NoteItemUiState(
    val title: String,
    val content: String,
    val creationDate: Date,
    val lastEditTime: Date,
) {
    /**
     * Creates a [NoteItemUiState] from a [Note].
     */
    constructor(note: Note) : this(
        note.title,
        note.content,
        note.creationDate,
        note.lastEditTime
    )

    /**
     * Converts this [NoteItemUiState] to a [Note].
     */
    fun toNote() = Note(title, content, creationDate, lastEditTime)

    companion object {

        /**
         * Finds a [NoteItemUiState] in a list of [NoteItemUiState]s by its creation date.
         *
         * @param creationDate The creation date of the note to find.
         * @return The [NoteItemUiState] with the given creation date, or `null` if no such note exists.
         */
        fun List<NoteItemUiState>.find(creationDate: Date): NoteItemUiState? {
            for (note in this) {
                if (note.creationDate == creationDate) return note
            }

            return null
        }
    }
}