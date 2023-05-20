package com.huggets.mynotes.data

import android.content.Context

class DeletedNoteRepository(context: Context) {
    private val deletedNoteDao = ApplicationDatabase.getDb(context).deletedNoteDao()

    suspend fun insert(deletedNote: DeletedNote) = deletedNoteDao.insert(deletedNote)

    suspend fun delete(creationDate: Date) = deletedNoteDao.delete(creationDate)

    suspend fun getAllDeletedNotes() = deletedNoteDao.getAllDeletedNotes()
}