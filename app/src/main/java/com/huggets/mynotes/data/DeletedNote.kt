package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a note that has been deleted.
 *
 * @property creationDate The creationDate of the note that has been deleted.
 */
@Entity(tableName = "deleted_note")
data class DeletedNote(
    @PrimaryKey
    @ColumnInfo(name = "creation_date")
    var creationDate: Date,
)