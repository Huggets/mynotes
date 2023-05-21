package com.huggets.mynotes.data

import android.content.Context

class NoteAssociationRepository(context: Context) {
    private val noteAssociationDao = ApplicationDatabase.getDb(context).noteAssociationDao()

    fun syncAllAssociations() = noteAssociationDao.getAllAssociationsFlow()

    suspend fun getAllAssociations() = noteAssociationDao.getAllAssociations()

    suspend fun insert(vararg noteAssociations: NoteAssociation) =
        noteAssociationDao.insert(*noteAssociations)

    suspend fun deleteByChildCreationDate(childCreationDate: Date) =
        noteAssociationDao.deleteByChildCreationDate(childCreationDate)
}