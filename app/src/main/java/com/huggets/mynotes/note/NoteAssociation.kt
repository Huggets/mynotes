package com.huggets.mynotes.note

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

// TODO Implements indices

@Entity(
    tableName = "note_association",
    primaryKeys = ["parent_id", "child_id"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class NoteAssociation(
    @ColumnInfo(name = "parent_id")
    var parentId: Long,

    @ColumnInfo(name = "child_id")
    var childId: Long,
)
