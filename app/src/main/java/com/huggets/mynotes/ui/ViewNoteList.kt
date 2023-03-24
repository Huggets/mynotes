package com.huggets.mynotes.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huggets.mynotes.*
import com.huggets.mynotes.note.NoteAppUiState
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNoteList(
    quitApplication: () -> Unit,
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    deleteNotes: (List<Long>) -> Unit,
) {
    val saver = Saver<SnapshotStateMap<Long, Boolean>, String>(
        save = {
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
        },
        restore = {
            val map = SnapshotStateMap<Long, Boolean>()
            val items = it.split(';')
            if (items[0] != "") {
                for (item in items) {
                    val (id, value) = item.split(':')
                    map[id.toLong()] = value.toBoolean()
                }
            }

            map
        }
    )

    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val deleteSelectedNote: () -> Unit = { showDeleteConfirmation.value = true }

    val isSelectingMode = rememberSaveable { mutableStateOf(false) }
    val isNoteSelected = rememberSaveable(saver = saver) { mutableStateMapOf() }
    val selectedCount = rememberSaveable { mutableStateOf(0) }

    val onBackPressed = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSelectingMode.value) {
                    // Unselect all notes
                    isSelectingMode.value = false
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
            lifecycleOwner,
            onBackPressed
        )

        onDispose {
            onBackPressed.remove()
        }
    }

    BoxWithConstraints {
        val fabPosition =
            if (this.maxWidth < Value.Limit.minWidthRequiredFabToLeft) FabPosition.Center
            else FabPosition.End

        Scaffold(
            topBar = { ViewNoteListAppBar(isSelectingMode.value, deleteSelectedNote) },
            floatingActionButton = {
                if (!isSelectingMode.value) {
                    ViewNoteListFab(navigationController, this)
                }
            },
            floatingActionButtonPosition = fabPosition,
        ) { padding ->
            NoteList(
                navigationController,
                appState,
                isSelectingMode,
                isNoteSelected,
                selectedCount,
                Modifier.padding(padding)
            )

            ConfirmationDialog(
                displayDialog = showDeleteConfirmation,
                onConfirmation = {
                    selectedCount.value = 0
                    isSelectingMode.value = false

                    val toDelete = mutableListOf<Long>()

                    for ((noteId, isSelected) in isNoteSelected.entries) {
                        if (isSelected) {
                            toDelete.add(noteId)
                            isNoteSelected[noteId] = false // Unselect the note
                        }
                    }

                    deleteNotes(toDelete)
                },
                confirmationMessage = "Are you sure you want to delete the selected note(s)?"
            )
        }
    }
}


@Composable
private fun NoteList(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    isSelectingMode: MutableState<Boolean>,
    isNoteSelected: SnapshotStateMap<Long, Boolean>,
    selectedCount: MutableState<Int>,
    modifier: Modifier = Modifier,
) {
    val onClick: (Long) -> Unit = { id ->
        if (isSelectingMode.value) {
            if (isNoteSelected[id] != true) {
                isNoteSelected[id] = true
                selectedCount.value++
            } else {
                isNoteSelected[id] = false
                selectedCount.value--
                if (selectedCount.value == 0) {
                    isSelectingMode.value = false
                }
            }
        } else {
            navigationController.navigate(Destinations.generateEditNoteDestination(false, id))
        }
    }
    val onLongClick: (Long) -> Unit = { id ->
        isSelectingMode.value = true

        if (isNoteSelected[id] != true) {
            // If not already selected
            isNoteSelected[id] = true
            selectedCount.value++
        }
    }

    if (appState.value.items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(Value.smallPadding)
        ) {
            Text(
                text = "No notes",
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

    } else {
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            for (note in appState.value.items) {
                item(key = note.id) {
                    NoteElement(note, isNoteSelected[note.id], onClick, onLongClick)
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
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color: Color
    val contentColor: Color
    val selectionColor: Color
    if (isSystemInDarkTheme()) {
        color = md_theme_dark_surfaceVariant
        contentColor = md_theme_dark_onSurfaceVariant
        selectionColor = md_theme_dark_inverseSurface
    } else {
        color = md_theme_light_surfaceVariant
        contentColor = md_theme_light_onSurfaceVariant
        selectionColor = md_theme_light_inverseSurface
    }

    Surface(
        color = color,
        contentColor = contentColor,
        shape = ShapeDefaults.Small,
        modifier = modifier
            .padding(
                Value.smallPadding,
                0.dp,
                Value.smallPadding,
                Value.smallPadding,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(note.id) },
                    onLongClick = { onLongClick(note.id) }
                ),
        ) {
            Column(
                modifier = Modifier.padding(Value.smallPadding)
            ) {
                val title = note.title.let {
                    if (it.isBlank()) "No title"
                    else shortened(it, 20)
                }
                val content = shortened(note.content, 120)

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = content,
                    fontSize = 16.sp,
                )
            }
            Surface(
                color = selectionColor,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(
                        if (isSelected == true) 0.5f
                        else 0f
                    ),
            ) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewNoteListAppBar(
    isSelectingMode: Boolean,
    deleteSelectedNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text("View notes") },
        actions = {
            if (isSelectingMode) {
                IconButton(onClick = deleteSelectedNote) {
                    Icon(Icons.Filled.Delete, "Delete selected notes")
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ViewNoteListFab(
    navigationController: NavHostController,
    constraintsScope: BoxWithConstraintsScope,
) {
    val openNewNote: () -> Unit = {
        navigationController.navigate(
            Destinations.generateEditNoteDestination(true)
        )
    }
    val label = "Add a new note"
    val icon: @Composable () -> Unit = { Icon(Icons.Filled.Add, label) }

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