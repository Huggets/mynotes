package com.huggets.mynotes.note

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var title: String,
    var content: String
)