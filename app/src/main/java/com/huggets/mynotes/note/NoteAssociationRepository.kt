package com.huggets.mynotes.note

import android.content.Context
import com.huggets.mynotes.ApplicationDatabase

class NoteAssociationRepository(context: Context) {
    private val noteAssociationDao = ApplicationDatabase.getDb(context).noteAssociationDao()

    fun fetchAll() = noteAssociationDao.getAll()

    suspend fun save(noteAssociation: NoteAssociation) = noteAssociationDao.insert(noteAssociation)
}