package com.huggets.mynotes.ui

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
        "$newNote?${ParametersName.parentNoteCreationDate}={${ParametersName.parentNoteCreationDate}}"
    const val editNoteRoute =
        "$editNote/{${ParametersName.noteCreationDate}}?${ParametersName.parentNoteCreationDate}={${ParametersName.parentNoteCreationDate}}"

    /**
     * Generate a route to the view note list destination
     */
    fun generateViewNoteList() = viewNoteList

    /**
     * Generate a route to the edit note destination.
     *
     * @param noteCreationDate If noteCreationDate is null, then the destination is for a new note.
     * @param parentCreationDate If parentCreationDate is null, then the note will be a root note,
     * otherwise it will be associated to another the parent note.
     */
    fun generateEditNoteDestination(
        noteCreationDate: String?,
        parentCreationDate: String?
    ): String {
        val parent: String? =
            if (parentCreationDate == null) null
            else "${ParametersName.parentNoteCreationDate}=$parentCreationDate"

        return if (noteCreationDate != null) {
            "$editNote/$noteCreationDate?$parent"
        } else {
            "$newNote?$parent"
        }
    }
}