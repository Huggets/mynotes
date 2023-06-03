package com.huggets.mynotes.ui


import com.huggets.mynotes.data.Date

/**
 * Contains useful constants and functions to navigate between destinations.
 */
object Destinations {

    /**
     * Contains the name of the parameters used to navigate between destinations.
     */
    object ParametersName {

        /**
         * The creation date of the note. Used to navigate to the edit note and new note
         * destinations.
         */
        const val noteCreationDate = "noteCreationDate"
    }

    private const val viewNoteList = "viewNoteList"
    private const val editNote = "editNote"
    private const val newNote = "newNote"
    private const val dataSynchronization = "dataSynchronization"

    /**
     * The route to the view note list destination.
     */
    const val viewNoteListRoute = viewNoteList

    /**
     * The route to the new note destination.
     */
    const val newNoteRoute = "$newNote/{${ParametersName.noteCreationDate}}"

    /**
     * The route to the edit note destination.
     */
    const val editNoteRoute = "$editNote/{${ParametersName.noteCreationDate}}"

    /**
     * The route to the data synchronization destination.
     */
    const val dataSyncingRoute = dataSynchronization

    /**
     * Generates a route to the view note list destination.
     */
    fun generateViewNoteList() = viewNoteList

    /**
     * Generates a route to the edit note destination.
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

    /**
     * Generates a route to the data synchronization destination.
     */
    fun generateDataSyncing() = dataSynchronization
}