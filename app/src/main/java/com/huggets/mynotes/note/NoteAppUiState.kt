package com.huggets.mynotes.note

data class NoteAppUiState(
    val items: List<NoteItemUiState> = listOf(),
    val isFetchingItem: Boolean = false,
)
