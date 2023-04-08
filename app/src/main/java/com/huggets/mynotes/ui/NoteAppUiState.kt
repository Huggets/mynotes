package com.huggets.mynotes.ui

data class NoteAppUiState(
    val allNotes: List<NoteItemUiState> = listOf(),
    val mainNoteIds: List<Long> = listOf(),
    val noteAssociations: List<NoteAssociationItemUiState> = listOf(),
)
