package com.huggets.mynotes.note

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM note WHERE id = :noteId")
    suspend fun delete(noteId: Long): Int

    @Query("SELECT * FROM note")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT note.id FROM note WHERE note.id NOT IN (SELECT DISTINCT child_id FROM note_association)")
    fun getMainNotes(): Flow<List<Long>>

    /**
     * Get all children of a note recursively
     */
    @Query(
        """
        WITH RECURSIVE children(child_id) AS (
          SELECT child_id FROM note_association WHERE parent_id = :parentId
          UNION ALL
          SELECT note_association.child_id FROM note_association
            JOIN children ON note_association.parent_id = children.child_id
        )
        SELECT note.* FROM note JOIN children ON note.id = children.child_id
        """
    )
    suspend fun getChildren(parentId: Long): List<Note>
}