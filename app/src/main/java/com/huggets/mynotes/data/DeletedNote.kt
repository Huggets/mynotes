package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_note")
data class DeletedNote(
    @PrimaryKey
    @ColumnInfo(name = "creation_date")
    var creationDate: Date,
    @ColumnInfo(name = "deletion_date")
    var deletionDate: Date,
)