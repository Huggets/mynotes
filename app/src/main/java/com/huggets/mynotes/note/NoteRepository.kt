package com.huggets.mynotes.note

import android.content.Context
import com.huggets.mynotes.ApplicationDatabase

class NoteRepository(context: Context) {

    private val noteDao = ApplicationDatabase.getDb(context).noteDao()

    suspend fun insert(note: Note) = noteDao.insert(note)

    suspend fun update(note: Note) = noteDao.update(note)

    suspend fun delete(noteCreationDate: String) = noteDao.delete(noteCreationDate) > 0

    fun syncAllNotes() = noteDao.getAllNotesFlow()

    suspend fun getAllNotes() = noteDao.getAllNotes()

    fun syncMainNotes() = noteDao.getMainNotesFlow()

    /**
     * Get all children of a note recursively
     */
    suspend fun getChildren(parentCreationDate: String) = noteDao.getChildren(parentCreationDate)
}