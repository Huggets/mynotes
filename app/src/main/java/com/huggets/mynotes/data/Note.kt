package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "note",
    primaryKeys = ["creation_date"],
)
data class Note(
    var title: String,
    var content: String,
    @ColumnInfo(name = "creation_date")
    var creationDate: String,
    @ColumnInfo(name = "last_edit_time")
    var lastEditTime: String,
)