package com.huggets.mynotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: Int): Int

    @Query("DELETE FROM note WHERE creation_date = :creationDate")
    suspend fun delete(creationDate: Date): Int

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun get(id: Int): Note?

    @Query("SELECT * FROM note")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM note")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT note.id FROM note WHERE note.id NOT IN (SELECT DISTINCT child_id FROM note_association) ORDER BY note.last_edit_time DESC")
    fun getMainNotesFlow(): Flow<List<Int>>

    /**
     * Get all children of a note recursively
     */
    @Query(
        """
        WITH RECURSIVE children(child_id) AS (
          SELECT child_id FROM note_association WHERE parent_id = :id
          UNION ALL
          SELECT note_association.child_id FROM note_association
            JOIN children ON note_association.parent_id = children.child_id
        )
        SELECT note.* FROM note JOIN children ON note.id = children.child_id
        """
    )
    suspend fun getChildren(id: Int): List<Note>

    @Query("SELECT MAX(note.id) FROM note")
    suspend fun getGreatestIdUsed(): Int?
}