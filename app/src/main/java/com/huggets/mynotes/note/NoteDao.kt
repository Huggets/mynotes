package com.huggets.mynotes.note

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note): Int

    @Query("DELETE FROM note WHERE id = :noteId")
    suspend fun delete(noteId: Long): Int

    @Query("SELECT * FROM note")
    fun getAllNotes(): Flow<List<Note>>
}