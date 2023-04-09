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
import com.huggets.mynotes.theme.*

private val emphasizedFloat = Value.Animation.emphasized<Float>()
private val saver = Saver<SnapshotStateMap<Long, Boolean>, String>(save = {
    val builder = StringBuilder()
    for ((id, value) in it) {
        builder.append(id)
        builder.append(':')
        builder.append(value)
        builder.append(';')
    }
    if (builder.lastIndex != -1) {
        builder.delete(builder.lastIndex, builder.lastIndex + 1)
    }
    builder.toString()
}, restore = {
    val map = SnapshotStateMap<Long, Boolean>()
    val items = it.split(';')
    if (items[0] != "") {
        for (item in items) {
            val (id, value) = item.split(':')
            map[id.toLong()] = value.toBoolean()
        }
    }

    map
})

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteList(
    quitApplication: () -> Unit,
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    fabPosition: MutableState<FabPosition>,
    deleteNotes: (List<Long>) -> Unit,
    exportToXml: () -> Unit,
    importFromXml: () -> Unit,
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

    val onBackPressed = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectionMode.value) {
                    // Unselect all notes
                    selectionMode.value = false
                    selectedCount.value = 0

                    for (noteId in isNoteSelected.keys) {
                        isNoteSelected[noteId] = false
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
                )
            },
            floatingActionButton = {
                Fab(navigationController, this, fabTransitionState)
            },
            floatingActionButtonPosition = fabPosition.value,
        ) { padding ->
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

                    val toDelete = mutableListOf<Long>()

                    for ((noteId, isSelected) in isNoteSelected.entries) {
                        if (isSelected) {
                            toDelete.add(noteId)
                            isNoteSelected[noteId] = false // Unselect the note
                        }
                    }

                    deleteNotes(toDelete)
                }, confirmationMessage = "Are you sure you want to delete the selected note(s)?"
            )
        }
    }
}

@Composable
private fun NoteElementList(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    selectionMode: MutableState<Boolean>,
    isNoteSelected: SnapshotStateMap<Long, Boolean>,
    selectedCount: MutableState<Int>,
    fabTransitionState: MutableTransitionState<Boolean>,
    fabWasShown: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val onClick: (Long) -> Unit = { id ->
        if (selectionMode.value) {
            if (isNoteSelected[id] != true) {
                isNoteSelected[id] = true
                selectedCount.value++
            } else {
                isNoteSelected[id] = false
                selectedCount.value--
                if (selectedCount.value == 0) {
                    selectionMode.value = false
                }
            }
        } else {
            navigationController.navigate(Destinations.generateEditNoteDestination(id))
        }
    }
    val onLongClick: (Long) -> Unit = { id ->
        selectionMode.value = true

        if (isNoteSelected[id] != true) {
            // If not already selected
            isNoteSelected[id] = true
            selectedCount.value++
        }
    }

    if (appState.value.mainNoteIds.isEmpty()) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth()
                )
            }
            for (mainNoteId in appState.value.mainNoteIds) {
                item(key = mainNoteId) {
                    val note = appState.value.allNotes.find(mainNoteId)

                    if (note != null) {
                        NoteElement(
                            note,
                            isNoteSelected[note.id],
                            onClick,
                            onLongClick,
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteElement(
    note: NoteItemUiState,
    isSelected: Boolean?,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
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
                .combinedClickable(onClick = { onClick(note.id) },
                    onLongClick = { onLongClick(note.id) }),
        ) {
            Column(
                modifier = Modifier.padding(Value.smallPadding)
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
                modifier = Modifier.matchParentSize()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.5f),
                    content = {})
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
                        onClick = exportToXml,
                    )
                    DropdownMenuItem(
                        text = { Text("Import from XML") },
                        onClick = importFromXml,
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
) {
    val openNewNote: () -> Unit = {
        navigationController.navigate(
            Destinations.generateEditNoteDestination(0)
        )
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