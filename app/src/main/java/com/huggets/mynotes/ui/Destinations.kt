package com.huggets.mynotes.ui

object Destinations {
    object ParametersName {
        const val noteId = "noteId"
        // TODO Maybe remove this parameter
        const val parentId = "parentId"
    }

    private const val viewNoteList = "viewNoteList"
    private const val editNote = "editNote"
    private const val newNote = "newNote"

    const val viewNoteListRoute = viewNoteList
    const val newNoteRoute =
        "$newNote/{${ParametersName.noteId}}?${ParametersName.parentId}={${ParametersName.parentId}}"
    const val editNoteRoute =
        "$editNote/{${ParametersName.noteId}}?${ParametersName.parentId}={${ParametersName.parentId}}"

    /**
     * Generate a route to the view note list destination
     */
    fun generateViewNoteList() = viewNoteList

    /**
     * Generate a route to the edit note destination.
     *
     * @param noteId The id of the note to edit.
     * @param parentId If parentId is null, then the note will be a root note,
     * otherwise it will be associated to the parent note.
     * @param isNew If the note is a new note or an existing one.
     */
    fun generateEditNote(
        noteId: Int,
        parentId: Int?,
        isNew: Boolean,
    ): String {
        val parent: String? =
            if (parentId == null) null
            else "${ParametersName.parentId}=$parentId"

        return if (isNew) {
            "$newNote/$noteId?$parent"
        } else {
            "$editNote/$noteId?$parent"
        }
    }
}