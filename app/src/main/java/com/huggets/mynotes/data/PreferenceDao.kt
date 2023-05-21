package com.huggets.mynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg preferences: Preference)

    @Query("SELECT * FROM preference WHERE name = :name")
    suspend fun getPreference(name: String): Preference?
}