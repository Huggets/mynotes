package com.huggets.mynotes.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huggets.mynotes.*
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.theme.*
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import com.huggets.mynotes.ui.state.find

private val saver = Saver<SnapshotStateMap<Date, Boolean>, String>(save = {
    val builder = StringBuilder()
    for ((creationDate, value) in it) {
        builder.append(creationDate)
        builder.append("@")
        builder.append(value)
        builder.append(';')
    }
    if (builder.lastIndex != -1) {
        builder.delete(builder.lastIndex, builder.lastIndex + 1)
    }
    builder.toString()
}, restore = {
    val map = SnapshotStateMap<Date, Boolean>()
    val items = it.split(';')
    if (items[0] != "") {
        for (item in items) {
            val (creationDateString, value) = item.split('@')
            val creationDate = Date.fromString(creationDateString)
            map[creationDate] = value.toBoolean()
        }
    }

    map
})

private val emphasizedFloat = Value.Animation.emphasized<Float>()

@Composable
fun NoteListActivity(
    quitApplication: () -> Unit,
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    fabPosition: MutableState<FabPosition>,
    deleteNotes: (creationDates: List<Date>) -> Unit,
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit,
    exportToXml: () -> Unit,
    importFromXml: () -> Unit,
    startSyncDataWithAnotherDevice: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val deleteSelectedNote: () -> Unit = { showDeleteConfirmation.value = true }

    val selectionMode = rememberSaveable { mutableStateOf(false) }
    val isNoteSelected = rememberSaveable(saver = saver) { mutableStateMapOf() }
    val selectedCount = rememberSaveable { mutableStateOf(0) }

    val fabWasShown = rememberSaveable { mutableStateOf(true) }
    val fabTransitionState = remember { MutableTransitionState(fabWasShown.value) }
    val deleteIconTransitionState = remember { MutableTransitionState(selectionMode.value) }

    fabTransitionState.targetState = if (fabWasShown.value) !selectionMode.value else false
    deleteIconTransitionState.targetState = selectionMode.value
    if (!selectionMode.value) {
        fabWasShown.value = fabTransitionState.targetState
    }

    BackPressHandler(
        navigationController,
        quitApplication,
        selectionMode,
        isNoteSelected,
        selectedCount
    )

    BoxWithConstraints {
        fabPosition.value =
            if (this.maxWidth < Value.Limit.minWidthRequiredFabToLeft) FabPosition.Center
            else FabPosition.End

        Scaffold(
            topBar = {
                AppBar(
                    deleteSelectedNote = deleteSelectedNote,
                    deleteIconState = deleteIconTransitionState,
                    exportToXml = exportToXml,
                    importFromXml = importFromXml,
                    startSyncDataWithAnotherDevice = startSyncDataWithAnotherDevice,
                )
            },
            floatingActionButton = {
                Fab(navigationController, this, fabTransitionState, createNote)
            },
            floatingActionButtonPosition = fabPosition.value,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { padding ->
            var snackbarWasShown by rememberSaveable { mutableStateOf(false) }

            if (appState.value.isImporting) {
                snackbarWasShown = false
            }

            if (appState.value.importFailed && !snackbarWasShown) {
                LaunchedEffect(snackbarHostState) {
                    snackbarHostState.showSnackbar(appState.value.importFailedMessage)
                    snackbarWasShown = true
                }
            }

            NoteElementList(
                navigationController,
                appState,
                selectionMode,
                isNoteSelected,
                selectedCount,
                fabTransitionState,
                fabWasShown,
                Modifier.padding(padding)
            )

            ConfirmationDialog(
                displayDialog = showDeleteConfirmation, onConfirmation = {
                    selectedCount.value = 0
                    selectionMode.value = false

                    val toDelete = mutableListOf<Date>()

                    for ((noteCreationDate, isSelected) in isNoteSelected.entries) {
                        if (isSelected) {
                            toDelete.add(noteCreationDate)
                            isNoteSelected[noteCreationDate] = false // Unselect the note
                        }
                    }

                    deleteNotes(toDelete)
                }, message = "Are you sure you want to delete the selected note(s)?"
            )
        }
    }
}

@Composable
private fun BackPressHandler(
    navigationController: NavHostController,
    quitApplication: () -> Unit,
    selectionMode: MutableState<Boolean>,
    isNoteSelected: SnapshotStateMap<Date, Boolean>,
    selectedCount: MutableState<Int>,
) {
    val onBackPressed = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectionMode.value) {
                    // Unselect all notes
                    selectionMode.value = false
                    selectedCount.value = 0

                    for (noteCreationDate in isNoteSelected.keys) {
                        isNoteSelected[noteCreationDate] = false
                    }
                } else {
                    val navigationFailed = !navigationController.navigateUp()
                    if (navigationFailed) {
                        quitApplication()
                    }
                }
            }
        }
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher?.onBackPressedDispatcher?.addCallback(
            lifecycleOwner, onBackPressed
        )

        onDispose {
            onBackPressed.remove()
        }
    }
}

@Composable
private fun NoteElementList(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    selectionMode: MutableState<Boolean>,
    isNoteSelected: SnapshotStateMap<Date, Boolean>,
    selectedCount: MutableState<Int>,
    fabTransitionState: MutableTransitionState<Boolean>,
    fabWasShown: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val onClick: (noteCreationDate: Date) -> Unit = { noteCreationDate ->
        if (selectionMode.value) {
            if (isNoteSelected[noteCreationDate] != true) {
                isNoteSelected[noteCreationDate] = true
                selectedCount.value++
            } else {
                isNoteSelected[noteCreationDate] = false
                selectedCount.value--
                if (selectedCount.value == 0) {
                    selectionMode.value = false
                }
            }
        } else {
            navigationController.navigate(
                Destinations.generateEditNote(
                    noteCreationDate,
                    false,
                )
            )
        }
    }
    val onLongClick: (noteCreationDate: Date) -> Unit = { noteCreationDate ->
        selectionMode.value = true

        if (isNoteSelected[noteCreationDate] != true) {
            // If not already selected
            isNoteSelected[noteCreationDate] = true
            selectedCount.value++
        }
    }

    if (appState.value.isImporting) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(Value.smallPadding)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else if (appState.value.mainNoteCreationDates.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(Value.smallPadding)
        ) {
            Text(
                text = "No notes", fontSize = 20.sp, modifier = Modifier.align(Alignment.Center)
            )
        }

    } else {
        val listState = rememberLazyListState()
        val lastListElementIndex =
            rememberSaveable { mutableStateOf(listState.firstVisibleItemIndex) }

        if (listState.isScrollInProgress) {
            val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

            if (lastListElementIndex.value > currentIndex) {
                lastListElementIndex.value = currentIndex

                if (!selectionMode.value) {
                    fabTransitionState.targetState = true
                    fabWasShown.value = true
                }
            } else if (lastListElementIndex.value < currentIndex) {
                lastListElementIndex.value = currentIndex

                if (!selectionMode.value) {
                    fabTransitionState.targetState = false
                    fabWasShown.value = false
                }
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(Value.smallSpacing),
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp, Value.smallSpacing),
        ) {
            for (mainNoteCreationDate in appState.value.mainNoteCreationDates) {
                item(key = mainNoteCreationDate.hashCode()) {
                    val note = appState.value.allNotes.find(mainNoteCreationDate)

                    if (note != null) {
                        NoteElement(
                            note,
                            isNoteSelected[note.creationDate],
                            onClick,
                            onLongClick,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteElement(
    note: NoteItemUiState,
    isSelected: Boolean?,
    onClick: (noteCreationDate: Date) -> Unit,
    onLongClick: (noteCreationDate: Date) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = ShapeDefaults.Small,
        modifier = modifier.padding(Value.smallPadding, 0.dp),
    ) {
        val selectionState = remember { MutableTransitionState(isSelected == true) }
        selectionState.targetState = isSelected == true

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onClick(note.creationDate) },
                    onLongClick = { onLongClick(note.creationDate) }),
        ) {
            Column(
                modifier = Modifier.padding(Value.smallPadding),
            ) {
                Text(
                    text = note.title.ifBlank { "No title" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                Text(
                    text = note.content,
                    fontSize = 16.sp,
                    maxLines = 5,
                )
            }
            AnimatedVisibility(
                visibleState = selectionState,
                enter = fadeIn(emphasizedFloat),
                exit = fadeOut(emphasizedFloat),
                modifier = Modifier.matchParentSize(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.5f),
                    content = {},
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    deleteSelectedNote: () -> Unit,
    deleteIconState: MutableTransitionState<Boolean>,
    exportToXml: () -> Unit,
    importFromXml: () -> Unit,
    startSyncDataWithAnotherDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text("View notes") },
        actions = {
            AnimatedVisibility(
                visibleState = deleteIconState,
                enter = fadeIn(emphasizedFloat),
                exit = fadeOut(emphasizedFloat),
            ) {
                IconButton(onClick = deleteSelectedNote) {
                    Icon(Icons.Filled.Delete, "Delete selected notes")
                }
            }
            Row {
                var isExpanded by rememberSaveable { mutableStateOf(false) }

                IconButton(onClick = { isExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, "More options")

                }

                DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Export to XML") },
                        onClick = {
                            exportToXml()
                            isExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import from XML") },
                        onClick = {
                            importFromXml()
                            isExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Sync data with another device") },
                        onClick = {
                            startSyncDataWithAnotherDevice()
                            isExpanded = false
                        },
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Fab(
    navigationController: NavHostController,
    constraintsScope: BoxWithConstraintsScope,
    transitionState: MutableTransitionState<Boolean>,
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit,
) {
    val openNewNote: () -> Unit = {
        createNote(null) { newNoteCreationDate ->
            val destination = Destinations.generateEditNote(newNoteCreationDate, true)
            navigationController.navigate(destination)
        }
    }
    val label = "Add a new note"
    val icon: @Composable () -> Unit = { Icon(Icons.Filled.Add, label) }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = scaleIn(emphasizedFloat),
        exit = scaleOut(emphasizedFloat),
    ) {
        if (constraintsScope.maxWidth < Value.Limit.minWidthRequiredExtendedFab) {
            FloatingActionButton(onClick = openNewNote) {
                icon.invoke()
            }
        } else {
            ExtendedFloatingActionButton(onClick = openNewNote) {
                icon.invoke()
                Text(text = label)
            }
        }
    }
}