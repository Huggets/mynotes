package com.huggets.mynotes.data

import android.content.Context

class NoteRepository(context: Context) {

    private val noteDao = ApplicationDatabase.getDb(context).noteDao()

    suspend fun insert(note: Note) = noteDao.insert(note)

    suspend fun update(note: Note) = noteDao.update(note)

    suspend fun delete(id: Int) = noteDao.delete(id) > 0

    fun syncAllNotes() = noteDao.getAllNotesFlow()

    suspend fun getAllNotes() = noteDao.getAllNotes()

    fun syncMainNotes() = noteDao.getMainNotesFlow()

    /**
     * Get all children of a note recursively
     */
    suspend fun getChildren(id: Int) = noteDao.getChildren(id)
}