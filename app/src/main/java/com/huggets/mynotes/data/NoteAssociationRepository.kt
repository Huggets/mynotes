package com.huggets.mynotes.data

import android.content.Context
import com.huggets.mynotes.ApplicationDatabase

class NoteAssociationRepository(context: Context) {
    private val noteAssociationDao = ApplicationDatabase.getDb(context).noteAssociationDao()

    fun syncAllAssociations() = noteAssociationDao.getAllAssociationsFlow()

    suspend fun getAllAssociations() = noteAssociationDao.getAllAssociations()

    suspend fun insert(noteAssociation: NoteAssociation) =
        noteAssociationDao.insert(noteAssociation)
}