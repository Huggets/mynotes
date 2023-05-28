package com.huggets.mynotes.ui


import com.huggets.mynotes.data.Date

object Destinations {
    object ParametersName {
        const val noteCreationDate = "noteCreationDate"
    }

    private const val viewNoteList = "viewNoteList"
    private const val editNote = "editNote"
    private const val newNote = "newNote"
    private const val dataSyncing = "dataSyncing"

    const val viewNoteListRoute = viewNoteList
    const val newNoteRoute = "$newNote/{${ParametersName.noteCreationDate}}"
    const val editNoteRoute = "$editNote/{${ParametersName.noteCreationDate}}"
    const val dataSyncingRoute = dataSyncing

    /**
     * Generate a route to the view note list destination
     */
    fun generateViewNoteList() = viewNoteList

    /**
     * Generate a route to the edit note destination.
     *
     * @param noteCreationDate The creation date of the note to edit.
     * @param isNew If the note is a new note or an existing one.
     */
    fun generateEditNote(
        noteCreationDate: Date,
        isNew: Boolean,
    ): String {

        return if (isNew) {
            "$newNote/$noteCreationDate"
        } else {
            "$editNote/$noteCreationDate"
        }
    }

    fun generateDataSyncing() = dataSyncing
}