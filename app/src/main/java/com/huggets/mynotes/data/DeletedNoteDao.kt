package com.huggets.mynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data access object for [DeletedNote].
 */
@Dao
interface DeletedNoteDao {

    /**
     * Inserts a new deleted note or replaces an existing one.
     *
     * @param deletedNotes the deleted notes to be inserted or replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg deletedNotes: DeletedNote)

    /**
     * Deletes a deleted note.
     *
     * @param creationDate the creation date of the deleted note to be deleted.
     *
     * @return the number of deleted notes.
     */
    @Query("DELETE FROM deleted_note WHERE creation_date = :creationDate")
    suspend fun delete(creationDate: Date): Int

    /**
     * Returns all deleted notes.
     */
    @Query("SELECT * FROM deleted_note")
    suspend fun getAllDeletedNotes(): List<DeletedNote>
}
