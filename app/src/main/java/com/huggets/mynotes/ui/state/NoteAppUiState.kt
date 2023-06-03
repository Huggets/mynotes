package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date

data class NoteAppUiState(
    val allNotes: List<NoteItemUiState> = listOf(),
    val mainNoteCreationDates: List<Date> = listOf(),
    val noteAssociations: List<NoteAssociationItemUiState> = listOf(),
    val isImporting: Boolean = false,
    val importFailed: Boolean = false,
    val importFailedMessage: String = "",
    val isExporting: Boolean = false,
    val exportFailed: Boolean = false,
    val exportFailedMessage: String = "",
    val dataSyncingUiState: DataSyncingUiState = DataSyncingUiState(),
)
