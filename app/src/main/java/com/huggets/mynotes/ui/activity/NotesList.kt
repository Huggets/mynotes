package com.huggets.mynotes.ui.activity

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huggets.mynotes.R
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.ui.AnimatedFab
import com.huggets.mynotes.ui.AnimatedIconButton
import com.huggets.mynotes.ui.BackPressHandler
import com.huggets.mynotes.ui.ConfirmationDialog
import com.huggets.mynotes.ui.Values
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState.Companion.find
import kotlin.collections.List
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.mutableListOf
import kotlin.collections.set

/**
 * A list of the main notes (notes that do not have a parent).
 *
 * @param appState The state of the app.
 * @param fabPosition The position of the FAB.
 * @param quitApplication The function to call when the user wants to quit the application.
 * @param navigateUp The function to call when the user wants to navigate up. Returns true if the
 * navigation was successful, false otherwise.
 * @param navigateToNote The function to call when the user wants to navigate to a note.
 * @param createNote The function to call when the user wants to create a new note.
 * @param deleteNotes The function to call when the user wants to delete notes.
 * @param export The function to call when the user wants to export notes.
 * @param import The function to call when the user wants to import notes.
 * @param startSynchronization The function to call when the user wants to start the synchronization
 * with another device.
 */
@Composable
fun NotesList(
    appState: State<NoteAppUiState>,
    fabPosition: MutableState<FabPosition>,
    quitApplication: () -> Unit = {},
    navigateUp: () -> Boolean = { false },
    navigateToNote: (noteCreationDate: Date, isNew: Boolean) -> Unit = { _, _ -> },
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit = { _, _ -> },
    deleteNotes: (creationDates: List<Date>) -> Unit = {},
    export: () -> Unit = {},
    import: () -> Unit = {},
    startSynchronization: () -> Unit = {},
) {
    val inSelectionMode = rememberSaveable { mutableStateOf(false) }
    val notesSelectionState = rememberSaveable(saver = selectedNotesSaver) { mutableStateMapOf() }
    val selectedNotesCount = rememberSaveable { mutableIntStateOf(0) }

    BackPressHandler {
        if (inSelectionMode.value) {
            // Unselect all notes
            inSelectionMode.value = false
            selectedNotesCount.intValue = 0

            notesSelectionState.keys.forEach {
                notesSelectionState[it] = false
            }
        } else {
            val navigateUpFailed = !navigateUp()
            if (navigateUpFailed) {
                quitApplication()
            }
        }
    }

    BoxWithConstraints {
        val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
        var shouldFabBeShown by rememberSaveable { mutableStateOf(true) }
        val snackbarHostState = remember { SnackbarHostState() }

        fabPosition.value =
            if (maxWidth < Values.minWidthRequiredFabToLeft)
                FabPosition.Center
            else
                FabPosition.End

        Scaffold(
            topBar = {
                AppBar(
                    isDeleteIconVisible = { inSelectionMode.value },
                    onDelete = { showDeleteConfirmation.value = true },
                    onExport = export,
                    onImport = import,
                    startSynchronization = startSynchronization,
                )
            },
            floatingActionButton = {
                val label = stringResource(R.string.add_new_note)

                AnimatedFab(
                    text = label,
                    icon = { Icon(Icons.Filled.Add, label) },
                    isVisible = { shouldFabBeShown && !inSelectionMode.value },
                    parentMaxWidth = maxWidth,
                    onClick = {
                        createNote(null) { newNoteCreationDate ->
                            navigateToNote(newNoteCreationDate, true)
                        }
                    }
                )
            },
            floatingActionButtonPosition = fabPosition.value,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { padding ->
            var importSnackbarShown by rememberSaveable(appState.value.importFailed) {
                mutableStateOf(appState.value.importFailed)
            }
            var exportSnackbarShown by rememberSaveable(appState.value.exportFailed) {
                mutableStateOf(appState.value.exportFailed)
            }

            LaunchedEffect(appState.value.importFailed) {
                if (appState.value.importFailed && importSnackbarShown) {
                    snackbarHostState.showSnackbar(appState.value.importFailedMessage)
                    importSnackbarShown = false
                }
            }
            LaunchedEffect(appState.value.exportFailed) {
                if (appState.value.exportFailed && exportSnackbarShown) {
                    snackbarHostState.showSnackbar(appState.value.exportFailedMessage)
                    exportSnackbarShown = false
                }
            }

            ConfirmationDialog(
                displayDialog = showDeleteConfirmation,
                message = stringResource(R.string.confirmation_message_delete_selected_notes),
                onConfirm = {
                    selectedNotesCount.intValue = 0
                    inSelectionMode.value = false

                    val toDelete = mutableListOf<Date>()

                    notesSelectionState.entries.forEach { (noteCreationDate, isSelected) ->
                        if (isSelected) {
                            toDelete.add(noteCreationDate)
                            notesSelectionState[noteCreationDate] = false // Unselect the note
                        }
                    }

                    deleteNotes(toDelete)
                },
            )

            MainContent(
                appState = appState,
                inSelectionMode = inSelectionMode,
                notesSelectionState = notesSelectionState,
                selectedNotesCount = selectedNotesCount,
                navigateToNote = navigateToNote,
                modifier = Modifier.padding(padding),
                onScrollUp = { shouldFabBeShown = true },
                onScrollDown = { shouldFabBeShown = false },
            )
        }
    }
}

/**
 * The main content of the NoteListActivity.
 *
 * @param appState The state of the app.
 * @param inSelectionMode Whether the user is in selection mode or not.
 * @param notesSelectionState The state of the selection of each note.
 * @param selectedNotesCount The number of selected notes.
 * @param modifier The modifier to apply to the content.
 * @param navigateToNote The function to call when the user wants to navigate to a note.
 * @param onScrollUp Called when the user scrolls up.
 * @param onScrollDown Called when the user scrolls down.
 */
@Composable
private fun MainContent(
    appState: State<NoteAppUiState>,
    inSelectionMode: MutableState<Boolean>,
    notesSelectionState: SnapshotStateMap<Date, Boolean>,
    selectedNotesCount: MutableState<Int>,
    modifier: Modifier = Modifier,
    navigateToNote: (noteCreationDate: Date, isNew: Boolean) -> Unit = { _, _ -> },
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {},
) {
    if (appState.value.isImporting) {
        Box(maxSizeWithPaddingModifier) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else if (appState.value.mainNoteCreationDates.isEmpty()) {
        Box(maxSizeWithPaddingModifier) {
            Text(
                text = stringResource(R.string.no_notes),
                fontSize = Values.bigFontSize,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        val onNoteClicked: (Date) -> Unit = { noteCreationDate: Date ->
            if (inSelectionMode.value) {
                if (notesSelectionState[noteCreationDate] != true) {
                    notesSelectionState[noteCreationDate] = true
                    selectedNotesCount.value++
                } else {
                    notesSelectionState[noteCreationDate] = false
                    selectedNotesCount.value--
                    if (selectedNotesCount.value == 0) {
                        inSelectionMode.value = false
                    }
                }
            } else {
                navigateToNote(noteCreationDate, false)
            }
        }
        val onNoteLongClicked: (Date) -> Unit = { noteCreationDate: Date ->
            inSelectionMode.value = true

            if (notesSelectionState[noteCreationDate] != true) {
                // If not already selected
                notesSelectionState[noteCreationDate] = true
                selectedNotesCount.value++
            }
        }
        val isNoteSelected = { noteCreationDate: Date ->
            notesSelectionState[noteCreationDate] == true
        }

        NotesList(
            notesToDisplay = { appState.value.mainNoteCreationDates },
            notes = { appState.value.allNotes },
            modifier = modifier,
            onNoteClicked = onNoteClicked,
            onNoteLongClicked = onNoteLongClicked,
            isNoteSelected = isNoteSelected,
            onScrollUp = onScrollUp,
            onScrollDown = onScrollDown,
        )
    }
}

/**
 * A list of notes.
 *
 * @param notesToDisplay The creation dates of the notes to display. For each creation date in this
 * list, a note with the same creation date must exist in [notes].
 * @param notes A list of all the notes. For each dates in [notesToDisplay], a note with the same
 * creation date must exist in this list.
 * @param modifier The modifier to apply to this layout.
 * @param onNoteClicked Called when a note is clicked.
 * @param onNoteLongClicked Called when a note is long clicked.
 * @param isNoteSelected A lambda that returns true if the note is selected.
 * @param onScrollUp Called when the user scrolls up.
 * @param onScrollDown Called when the user scrolls down.
 */
@Composable
private fun NotesList(
    notesToDisplay: () -> List<Date>,
    notes: () -> List<NoteItemUiState>,
    modifier: Modifier = Modifier,
    onNoteClicked: (Date) -> Unit = {},
    onNoteLongClicked: (Date) -> Unit = {},
    isNoteSelected: (Date) -> Boolean = { false },
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {},
) {
    // TODO Find a way to optimize the fab hiding/showing (for example by using a lambda)

    val listState = rememberLazyListState()

    val lastListElementIndex = rememberSaveable {
        mutableIntStateOf(listState.firstVisibleItemIndex)
    }

    if (listState.isScrollInProgress) {
        val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

        if (lastListElementIndex.intValue > currentIndex || currentIndex == 0) {
            lastListElementIndex.intValue = currentIndex
            onScrollUp()
        } else if (lastListElementIndex.intValue < currentIndex) {
            lastListElementIndex.intValue = currentIndex
            onScrollDown()
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Values.smallSpacing),
        modifier = maxWidthModifier.then(modifier),
        contentPadding = PaddingValues(0.dp, Values.smallSpacing),
    ) {
        val allNotes = notes()

        notesToDisplay().forEach {
            val note = allNotes.find(it)

            if (note != null) {
                item(it.hashCode()) {
                    NoteElement(
                        note = note,
                        isSelected = { isNoteSelected(it) },
                        onClick = onNoteClicked,
                        onLongClick = onNoteLongClicked,
                    )
                }
            } else {
                Log.d("NotesList", "------------\nNOTE IS NULL\n------------")
            }
        }
    }
}

/**
 * A single note element that can be selected, clicked and long clicked.
 *
 * @param note The note to display.
 * @param isSelected A lambda that returns true if the note is selected.
 * @param onClick A lambda that is called when the note is clicked.
 * @param onLongClick A lambda that is called when the note is long clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteElement(
    note: NoteItemUiState,
    isSelected: () -> Boolean = { false },
    onClick: (noteCreationDate: Date) -> Unit = {},
    onLongClick: (noteCreationDate: Date) -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = ShapeDefaults.Small,
        modifier = horizontalPaddingModifier,
    ) {
        Box(
            maxWidthModifier
                .combinedClickable(
                    onClick = { onClick(note.creationDate) },
                    onLongClick = { onLongClick(note.creationDate) },
                    role = Role.Button,
                ),
        ) {
            AnimatedSelectionSurface(isVisible = isSelected, modifier = Modifier.matchParentSize())

            Column(smallPaddingModifier) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.no_title) },
                    fontSize = Values.normalFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                Text(
                    text = note.content,
                    fontSize = Values.normalFontSize,
                    maxLines = 5,
                )
            }
        }
    }
}

/**
 * An animated selection surface.
 *
 * @param isVisible Whether the selection should be visible.
 * @param modifier The modifier to apply to the surface.
 */
@Composable
private fun AnimatedSelectionSurface(
    isVisible: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible(),
        enter = fadeIn(Values.emphasizedFloat),
        exit = fadeOut(Values.emphasizedFloat),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            modifier = modifier.alpha(0.5f),
            content = {},
        )
    }
}

/**
 * The app bar of the note list screen.
 *
 * @param isDeleteIconVisible Returns true if the delete icon should be visible.
 * @param onDelete Called when the delete icon is clicked.
 * @param onExport Called when the export icon is clicked.
 * @param onImport Called when the import icon is clicked.
 * @param startSynchronization Called when the synchronize icon is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    isDeleteIconVisible: () -> Boolean = { true },
    onDelete: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    startSynchronization: () -> Unit = {},
) {
    TopAppBar(
        title = { Text(stringResource(R.string.note_list_activity_name)) },
        actions = {
            AppBarActions(
                isDeleteIconVisible = isDeleteIconVisible,
                onDelete = onDelete,
                onExport = onExport,
                onImport = onImport,
                synchronizeWithAnotherDevice = startSynchronization,
            )
        },
    )
}

/**
 * The actions that can be performed in the app bar. They are:
 * - Delete selected notes,
 * - Export to XML,
 * - Import from XML,
 * - Synchronize data with another device.
 *
 * @param isDeleteIconVisible Whether the delete icon should be visible.
 * @param onDelete Called when the delete icon is clicked.
 * @param onExport Called when the export action is clicked.
 * @param onImport Called when the import action is clicked.
 * @param synchronizeWithAnotherDevice Called when the synchronize action is clicked.
 */
@Composable
private fun AppBarActions(
    isDeleteIconVisible: () -> Boolean = { true },
    onDelete: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    synchronizeWithAnotherDevice: () -> Unit = {},
) {
    AnimatedIconButton(
        visibleStateProvider = isDeleteIconVisible,
        onClick = onDelete,
    ) {
        Icon(Icons.Filled.Delete, stringResource(R.string.delete_selected_notes))
    }

    DropdownMenu(
        onExport = onExport,
        onImport = onImport,
        synchronizeWithAnotherDevice = synchronizeWithAnotherDevice,
    )
}

/**
 * A dropdown menu that contains the following actions:
 * - Export to XML,
 * - Import from XML,
 * - Synchronize data with another device.
 *
 * @param onExport The action to execute when the user clicks on the "export" item.
 * @param onImport The action to execute when the user clicks on the "import" item.
 * @param synchronizeWithAnotherDevice The action to execute when the user clicks on the
 * "synchronize" item.
 */
@Composable
private fun DropdownMenu(
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    synchronizeWithAnotherDevice: () -> Unit = {},
) {
    Box {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        IconButton(onClick = { isExpanded = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options))
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export_data)) },
                onClick = {
                    isExpanded = false
                    onExport()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.import_data)) },
                onClick = {
                    isExpanded = false
                    onImport()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.synchronize_data)) },
                onClick = {
                    isExpanded = false
                    synchronizeWithAnotherDevice()
                },
            )
        }
    }
}

/**
 * A [Saver] that saves and restores a [SnapshotStateMap] of [Date] and [Boolean] to and from a
 * [String].
 */
private val selectedNotesSaver = Saver<SnapshotStateMap<Date, Boolean>, String>(
    save = { stateMap ->
        StringBuilder().apply {
            stateMap.forEach { (creationDate, isSelected) ->
                append(creationDate)
                append("@")
                append(isSelected)
                append(';')
            }

            // Remove the last semicolon.
            if (lastIndex != -1) {
                delete(lastIndex, lastIndex + 1)
            }
        }.toString()
    }, restore = { savedString ->
        SnapshotStateMap<Date, Boolean>().apply {
            val items = savedString.split(';')

            if (items[0] != "") {
                items.forEach { item ->
                    val (creationDateString, isSelectedString) = item.split('@')
                    val creationDate = Date.fromString(creationDateString)
                    val isSelected = isSelectedString.toBoolean()

                    this[creationDate] = isSelected
                }
            }
        }
    }
)

/**
 * Modifier that adds a padding.
 */
private val smallPaddingModifier = Modifier
    .padding(Values.smallPadding)

/**
 * Modifier that adds a padding to the left and right.
 */
private val horizontalPaddingModifier = Modifier
    .padding(Values.smallPadding, 0.dp)

/**
 * Modifier that adds a padding and fills the maximum size of the parent.
 */
private val maxSizeWithPaddingModifier = Modifier
    .padding(Values.smallPadding)
    .fillMaxSize()

/**
 * Modifier that fills the maximum width of the parent.
 */
private val maxWidthModifier = Modifier
    .fillMaxWidth()
