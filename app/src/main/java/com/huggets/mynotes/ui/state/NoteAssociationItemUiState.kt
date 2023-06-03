package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.NoteAssociation

/**
 * Represents the state of a note association.
 *
 * @property parentCreationDate The creation date of the parent note.
 * @property childCreationDate The creation date of the child note.
 */
data class NoteAssociationItemUiState(
    val parentCreationDate: Date,
    val childCreationDate: Date,
) {

    /**
     * Creates a [NoteAssociationItemUiState] from a [NoteAssociation].
     */
    constructor(noteAssociation: NoteAssociation) : this(
        noteAssociation.parentCreationDate,
        noteAssociation.childCreationDate,
    )

    /**
     * Converts this [NoteAssociationItemUiState] to a [NoteAssociation].
     */
    fun toNoteAssociation() = NoteAssociation(parentCreationDate, childCreationDate)
}
