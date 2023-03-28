package com.huggets.mynotes.ui

data class NoteAppUiState(
    val items: List<NoteItemUiState> = listOf(),
    val isFetchingItem: Boolean = false,
)
