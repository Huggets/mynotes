package com.huggets.mynotes.ui

import com.huggets.mynotes.note.NoteAssociation

data class NoteAssociationItemUiState(
    val parentId: Long,
    val childId: Long,
) {
    constructor(noteAssociation: NoteAssociation) : this(
        noteAssociation.parentId,
        noteAssociation.childId
    )

    fun toNoteAssociation() = NoteAssociation(parentId, childId)
}
