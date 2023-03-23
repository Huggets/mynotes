package com.huggets.mynotes.note

import android.content.Context
import com.huggets.mynotes.ApplicationDatabase
import kotlinx.coroutines.flow.Flow

class NoteRepository(context: Context) {

    private val noteDao = ApplicationDatabase.getDb(context).noteDao()

    fun fetchNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes()
    }

    suspend fun saveNote(note: Note) {
        noteDao.insert(note)
    }

    suspend fun deleteNote(noteId: Long): Boolean {
        return noteDao.delete(noteId) > 0
    }
}