package com.huggets.mynotes.data

import android.content.Context

class NoteRepository(context: Context) {

    private val noteDao = ApplicationDatabase.getDb(context).noteDao()

    suspend fun insert(note: Note) =
        noteDao.insert(note.title, note.content, note.creationDate, note.lastEditTime)

    suspend fun update(note: Note) =
        noteDao.update(note.title, note.content, note.creationDate, note.lastEditTime)

    suspend fun delete(creationDate: Date) = noteDao.delete(creationDate) > 0

    suspend fun get(creationDate: Date) = noteDao.get(creationDate)

    fun syncAllNotes() = noteDao.getAllNotesFlow()

    suspend fun getAllNotes() = noteDao.getAllNotes()

    fun syncMainNotes() = noteDao.getMainNotesFlow()

    /**
     * Get all children of a note recursively
     */
    suspend fun getChildren(parentCreationDate: Date) = noteDao.getChildren(parentCreationDate)
}