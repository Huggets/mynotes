package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "note_association",
    primaryKeys = ["parent_creation_date", "child_creation_date"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["creation_date"],
            childColumns = ["parent_creation_date"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["creation_date"],
            childColumns = ["child_creation_date"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("child_creation_date")],
)
data class NoteAssociation(
    @ColumnInfo(name = "parent_creation_date")
    var parentCreationDate: Date,

    @ColumnInfo(name = "child_creation_date")
    var childCreationDate: Date,
)
