package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
    indices = [Index("child_id")],
)
data class NoteAssociation(
    @ColumnInfo(name = "parent_id")
    var parentId: Int,

    @ColumnInfo(name = "child_id")
    var childId: Int,
)
