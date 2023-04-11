package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.NoteAssociation

data class NoteAssociationItemUiState(
    val parentCreationDate: Date,
    val childCreationDate: Date,
) {
    constructor(noteAssociation: NoteAssociation) : this(
        noteAssociation.parentCreationDate,
        noteAssociation.childCreationDate,
    )

    fun toNoteAssociation() = NoteAssociation(parentCreationDate, childCreationDate)
}
