package com.huggets.mynotes.data

import android.content.Context

/**
 * Repository for [DeletedNotes][DeletedNote].
 *
 * @param context A context used to get the database.
 */
class DeletedNoteRepository(context: Context) {

    /**
     * The DAO for the deleted notes table.
     */
    private val deletedNoteDao = ApplicationDatabase.getDb(context).deletedNoteDao()

    /**
     * Inserts the given deleted notes into the database.
     *
     * @param deletedNotes The deleted notes to insert.
     */
    suspend fun insert(vararg deletedNotes: DeletedNote) = deletedNoteDao.insert(*deletedNotes)

    /**
     * Deletes the deleted notes with the given creation dates from the database.
     *
     * @param creationDate The creation dates of the deleted notes to delete.
     *
     * @return The number of deleted notes deleted.
     */
    suspend fun delete(creationDate: Date) = deletedNoteDao.delete(creationDate)

    /**
     * Returns all deleted notes from the database.
     */
    suspend fun getAllDeletedNotes() = deletedNoteDao.getAllDeletedNotes()
}