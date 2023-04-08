package com.huggets.mynotes.note

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAssociationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(noteAssociation: NoteAssociation)

    @Query("SELECT * FROM note_association")
    fun getAll(): Flow<List<NoteAssociation>>
}