package com.huggets.mynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preference")
data class Preference(
    @PrimaryKey
    var name: String,
    var value: String,
)