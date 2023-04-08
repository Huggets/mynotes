package com.huggets.mynotes.ui

object Destinations {
    object ParametersName {
        const val noteId = "noteId"
        const val parentNoteId = "parentId"
    }

    private const val viewNoteList = "viewNoteList"
    private const val editNote = "editNote"

    const val viewNoteListRoute = viewNoteList
    const val editNoteRoute = "$editNote/{${ParametersName.noteId}}/{${ParametersName.parentNoteId}}"

    /**
     * Generate a route to the view note list destination
     */
    fun generateViewNoteList() = viewNoteList

    /**
     * Generate a route the the edit note destination.
     *
     * By default, it edit a new note. If newNote is false then it edits
     * the note with the corresponding noteId.
     */
    fun generateEditNoteDestination(noteId: Long = 0, parentId: Long = 0) =
        "$editNote/$noteId/$parentId"
}