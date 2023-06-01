package com.huggets.mynotes.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository for [NoteAssociations][NoteAssociation].
 *
 * @param context The context used to get the database.
 */
class NoteAssociationRepository(context: Context) {

    /**
     * DAO for [NoteAssociations][NoteAssociation].
     */
    private val noteAssociationDao = ApplicationDatabase.getDb(context).noteAssociationDao()

    /**
     * Returns a [Flow] of all [NoteAssociations][NoteAssociation] in the database.
     */
    fun syncAllAssociations() = noteAssociationDao.getAllAssociationsFlow()

    /**
     * Returns all [NoteAssociations][NoteAssociation] in the database.
     */
    suspend fun getAllAssociations() = noteAssociationDao.getAllAssociations()

    /**
     * Inserts the given [NoteAssociations][NoteAssociation] into the database.
     */
    suspend fun insert(vararg noteAssociations: NoteAssociation) =
        noteAssociationDao.insert(*noteAssociations)

    /**
     * Deletes the given [NoteAssociations][NoteAssociation] from the database.
     *
     * @param childCreationDate The creation date of the child note.
     */
    suspend fun deleteByChildCreationDate(childCreationDate: Date) =
        noteAssociationDao.deleteByChildCreationDate(childCreationDate)
}