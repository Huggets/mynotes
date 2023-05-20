package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.NoteAssociation

data class NoteAssociationItemUiState(
    val parentId: Int,
    val childId: Int,
) {
    constructor(noteAssociation: NoteAssociation) : this(
        noteAssociation.parentId,
        noteAssociation.childId,
    )

    fun toNoteAssociation() = NoteAssociation(parentId, childId)
}
