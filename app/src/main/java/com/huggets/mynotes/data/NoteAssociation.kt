package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Represents a relationship between a parent note and a child note.
 *
 * A parent note can have multiple child notes, but a child note can only have one parent note.
 *
 * @property parentCreationDate The creation date of the parent note.
 * @property childCreationDate The creation date of the child note.
 */
@Entity(
    tableName = "note_association",
    primaryKeys = ["parent_creation_date", "child_creation_date"],
    indices = [Index("child_creation_date")],
)
data class NoteAssociation(
    @ColumnInfo(name = "parent_creation_date")
    var parentCreationDate: Date,

    @ColumnInfo(name = "child_creation_date")
    var childCreationDate: Date,
)
