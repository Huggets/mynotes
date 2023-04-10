package com.huggets.mynotes.ui.state

data class NoteAppUiState(
    val allNotes: List<NoteItemUiState> = listOf(),
    val mainNoteCreationDates: List<String> = listOf(),
    val noteAssociations: List<NoteAssociationItemUiState> = listOf(),
)
