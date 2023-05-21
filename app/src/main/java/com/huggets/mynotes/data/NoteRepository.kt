package com.huggets.mynotes.data

import android.content.Context

class NoteRepository(context: Context) {

    private val noteDao = ApplicationDatabase.getDb(context).noteDao()

    suspend fun insert(vararg notes: Note) = noteDao.insert(*notes)

    suspend fun update(vararg notes: Note) = noteDao.update(*notes)

    suspend fun delete(id: Int) = noteDao.delete(id) > 0

    suspend fun delete(creationDate: Date) = noteDao.delete(creationDate) > 0

    suspend fun get(id: Int) = noteDao.get(id)

    fun syncAllNotes() = noteDao.getAllNotesFlow()

    suspend fun getAllNotes() = noteDao.getAllNotes()

    fun syncMainNotes() = noteDao.getMainNotesFlow()

    /**
     * Get all children of a note recursively
     */
    suspend fun getChildren(id: Int) = noteDao.getChildren(id)

    suspend fun getGreatestIdUsed() = noteDao.getGreatestIdUsed()
}