package com.huggets.mynotes.ui.state

import com.huggets.mynotes.data.Date

/**
 * Represents the state of the app.
 *
 * @property allNotes The list of all notes.
 * @property mainNoteCreationDates The list of creation dates of the main notes.
 * @property noteAssociations The list of note associations.
 * @property isImporting Whether the app is importing data.
 * @property importFailed Whether the import failed.
 * @property importFailedMessage The import failed message if the import failed.
 * @property isExporting Whether the app is exporting data.
 * @property exportFailed Whether the export failed.
 * @property exportFailedMessage The export failed message if the export failed.
 * @property synchronizationState The state of the synchronization process.
 */
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
    val synchronizationState: SynchronizationUiState = SynchronizationUiState(),
)
