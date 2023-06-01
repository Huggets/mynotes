package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * Represents a note with title, content, creation date and last edit time (last modification date).
 *
 * @property title The title of the note.
 * @property content The content of the note.
 * @property creationDate The creation date of the note.
 * @property lastEditTime The last modification date of the note.
 */
@Fts4
@Entity(tableName = "note")
data class Note(
    var title: String,
    var content: String,
    @ColumnInfo(name = "creation_date")
    var creationDate: Date,
    @ColumnInfo(name = "last_edit_time")
    var lastEditTime: Date,
) {
    companion object {

        /**
         * Finds a note in a list of notes by its creation date.
         */
        fun List<Note>.find(creationDate: Date): Note? {
            return this.find { it.creationDate == creationDate }
        }
    }
}