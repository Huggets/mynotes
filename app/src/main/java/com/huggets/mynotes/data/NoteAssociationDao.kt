package com.huggets.mynotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAssociationDao {
    @Insert
    suspend fun insert(vararg noteAssociations: NoteAssociation)

    @Query("SELECT * FROM note_association")
    fun getAllAssociationsFlow(): Flow<List<NoteAssociation>>

    @Query("SELECT * FROM note_association")
    suspend fun getAllAssociations(): List<NoteAssociation>
}