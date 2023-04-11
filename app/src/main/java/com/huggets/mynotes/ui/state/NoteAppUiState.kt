package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date

data class NoteAppUiState(
    val allNotes: List<NoteItemUiState> = listOf(),
    val mainNoteCreationDates: List<Date> = listOf(),
    val noteAssociations: List<NoteAssociationItemUiState> = listOf(),
)
