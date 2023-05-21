package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

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
