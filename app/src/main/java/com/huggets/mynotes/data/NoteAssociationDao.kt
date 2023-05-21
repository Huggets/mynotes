package com.huggets.mynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAssociationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg noteAssociations: NoteAssociation)

    @Query("DELETE FROM note_association WHERE child_creation_date = :childCreationDate")
    suspend fun deleteByChildCreationDate(childCreationDate: Date)

    @Query("SELECT * FROM note_association")
    fun getAllAssociationsFlow(): Flow<List<NoteAssociation>>

    @Query("SELECT * FROM note_association")
    suspend fun getAllAssociations(): List<NoteAssociation>
}