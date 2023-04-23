package com.huggets.mynotes.ui

import com.huggets.mynotes.data.Date

object Destinations {
    object ParametersName {
        const val noteCreationDate = "noteCreationDate"
        const val parentNoteCreationDate = "parentCreationDate"
    }

    private const val viewNoteList = "viewNoteList"
    private const val editNote = "editNote"
    private const val newNote = "newNote"

    const val viewNoteListRoute = viewNoteList
    const val newNoteRoute =
        "$newNote/{${ParametersName.noteCreationDate}}?${ParametersName.parentNoteCreationDate}={${ParametersName.parentNoteCreationDate}}"
    const val editNoteRoute =
        "$editNote/{${ParametersName.noteCreationDate}}?${ParametersName.parentNoteCreationDate}={${ParametersName.parentNoteCreationDate}}"

    /**
     * Generate a route to the view note list destination
     */
    fun generateViewNoteList() = viewNoteList

    /**
     * Generate a route to the edit note destination.
     *
     * @param noteCreationDate The creation date of the note to edit.
     * @param parentCreationDate If parentCreationDate is null, then the note will be a root note,
     * otherwise it will be associated to the parent note.
     * @param isNew If the note is a new note or an existing one.
     */
    fun generateEditNote(
        noteCreationDate: Date,
        parentCreationDate: Date?,
        isNew: Boolean,
    ): String {
        val parent: String? =
            if (parentCreationDate == null) null
            else "${ParametersName.parentNoteCreationDate}=$parentCreationDate"

        return if (isNew) {
            "$newNote/$noteCreationDate?$parent"
        } else {
            "$editNote/$noteCreationDate?$parent"
        }
    }
}