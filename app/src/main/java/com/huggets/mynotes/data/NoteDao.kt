package com.huggets.mynotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        """
            INSERT INTO note (title, content, creation_date, last_edit_time)
            SELECT :title, :content, :creationDate, :lastEditTime
            WHERE NOT EXISTS (SELECT *  FROM note WHERE creation_date = :creationDate)
        """
    )
    suspend fun insert(title: String, content: String, creationDate: Date, lastEditTime: Date)


    @Query(
        """
        UPDATE note
        SET title = :title,
        content = :content,
        last_edit_time = :lastEditTime
        WHERE creation_date = :creationDate
        """
    )
    suspend fun update(title: String, content: String, creationDate: Date, lastEditTime: Date)

    @Query("DELETE FROM note WHERE creation_date = :creationDate")
    suspend fun delete(creationDate: Date): Int

    @Query("SELECT * FROM note WHERE creation_date = :creationDate")
    suspend fun get(creationDate: Date): Note?

    @Query("SELECT * from note WHERE title MATCH :query OR content MATCH :query")
    fun search(query: String): Flow<List<Note>>

    @Query("SELECT * FROM note")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM note")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT note.creation_date FROM note WHERE note.creation_date NOT IN (SELECT DISTINCT child_creation_date FROM note_association) ORDER BY note.last_edit_time DESC")
    fun getMainNotesFlow(): Flow<List<Date>>

    /**
     * Get all children of a note recursively
     */
    @Query(
        """
        WITH RECURSIVE children(child_creation_date) AS (
          SELECT child_creation_date FROM note_association WHERE parent_creation_date = :parentNoteCreationDate
          UNION ALL
          SELECT note_association.child_creation_date FROM note_association
            JOIN children ON note_association.parent_creation_date = children.child_creation_date
        )
        SELECT note.* FROM note JOIN children ON note.creation_date= children.child_creation_date
        """
    )
    suspend fun getChildren(parentNoteCreationDate: Date): List<Note>
}