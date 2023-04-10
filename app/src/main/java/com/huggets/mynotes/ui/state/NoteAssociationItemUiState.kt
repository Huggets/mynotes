package com.huggets.mynotes.ui.state

import com.huggets.mynotes.note.NoteAssociation

data class NoteAssociationItemUiState(
    val parentCreationDate: String,
    val childCreationDate: String,
) {
    constructor(noteAssociation: NoteAssociation) : this(
        noteAssociation.parentCreationDate,
        noteAssociation.childCreationDate,
    )

    fun toNoteAssociation() = NoteAssociation(parentCreationDate, childCreationDate)
}
