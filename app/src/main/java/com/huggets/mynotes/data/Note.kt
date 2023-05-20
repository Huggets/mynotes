package com.huggets.mynotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note")
data class Note(
    @PrimaryKey
    var id: Int,
    var title: String,
    var content: String,
    @ColumnInfo(name = "creation_date")
    var creationDate: Date,
    @ColumnInfo(name = "last_edit_time")
    var lastEditTime: Date,
)