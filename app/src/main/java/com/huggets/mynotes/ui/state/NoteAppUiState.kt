package com.huggets.mynotes.ui.state

data class NoteAppUiState(
    val allNotes: List<NoteItemUiState> = listOf(),
    val mainNoteIds: List<Int> = listOf(),
    val noteAssociations: List<NoteAssociationItemUiState> = listOf(),
)
