package com.huggets.mynotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [Note] entity
 */
@Dao
interface NoteDao {

    /**
     * Insert a note in the database only if the creation date is not already present.
     */
    @Query(
        """
            INSERT INTO note (title, content, creation_date, last_edit_time)
            SELECT :title, :content, :creationDate, :lastEditTime
            WHERE NOT EXISTS (SELECT *  FROM note WHERE creation_date = :creationDate)
        """
    )
    suspend fun insert(title: String, content: String, creationDate: Date, lastEditTime: Date)


    /**
     * Update a note in the database with the same creation date.
     */
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

    /**
     * Delete a note in the database with the same creation date.
     *
     * @return the number of notes deleted.
     */
    @Query("DELETE FROM note WHERE creation_date = :creationDate")
    suspend fun delete(creationDate: Date): Int

    /**
     * Get a note from the database with the same creation date.
     */
    @Query("SELECT * FROM note WHERE creation_date = :creationDate")
    suspend fun get(creationDate: Date): Note?


    @Query("SELECT * from note WHERE title MATCH :query OR content MATCH :query")
    fun search(query: String): Flow<List<Note>>

    /**
     * Get all notes from the database as a Flow.
     */
    @Query("SELECT * FROM note")
    fun getAllNotesFlow(): Flow<List<Note>>

    /**
     * Get all notes from the database.
     */
    @Query("SELECT * FROM note")
    suspend fun getAllNotes(): List<Note>

    /**
     * Get all main notes from the database as a Flow.
     *
     * A main note is a note that is not a child of another note.
     */
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