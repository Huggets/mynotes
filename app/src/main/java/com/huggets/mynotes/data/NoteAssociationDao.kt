package com.huggets.mynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [NoteAssociation] entity.
 */
@Dao
interface NoteAssociationDao {

    /**
     * Insert a note association in the database. If the note already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg noteAssociations: NoteAssociation)

    /**
     * Delete a note association by [childCreationDate].
     */
    @Query("DELETE FROM note_association WHERE child_creation_date = :childCreationDate")
    suspend fun deleteByChildCreationDate(childCreationDate: Date)


    /**
     * Returns all note associations as a [Flow].
     */
    @Query("SELECT * FROM note_association")
    fun getAllAssociationsFlow(): Flow<List<NoteAssociation>>

    /**
     * Returns all note associations.
     */
    @Query("SELECT * FROM note_association")
    suspend fun getAllAssociations(): List<NoteAssociation>
}