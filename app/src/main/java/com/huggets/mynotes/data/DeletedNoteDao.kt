package com.huggets.mynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeletedNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletedNote: DeletedNote)

    @Query("DELETE FROM deleted_note WHERE creation_date = :creationDate")
    suspend fun delete(creationDate: Date): Int

    @Query("SELECT * FROM deleted_note")
    suspend fun getAllDeletedNotes(): List<DeletedNote>
}
