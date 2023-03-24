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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val isDeleting = rememberSaveable { mutableStateOf(false) }
    val deleteSelectedNote: () -> Unit = { isDeleting.value = true }

    val isSelectingMode = rememberSaveable { mutableStateOf(false) }
    val isNoteSelected = remember { mutableStateMapOf<Long, Boolean>() }
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

            // TODO put the code in a function to be able to reuse it, especially in EditNote
            if (isDeleting.value) {
                val onDismiss: () -> Unit = { isDeleting.value = false }
                val onConfirm: () -> Unit = {
                    val toDelete = mutableListOf<Long>()

                    for ((noteId, isSelected) in isNoteSelected.entries) {
                        if (isSelected) {
                            toDelete.add(noteId)
                            isNoteSelected[noteId] = false // Unselect the note
                        }
                    }

                    selectedCount.value = 0
                    isSelectingMode.value = false
                    deleteNotes(toDelete)
                    isDeleting.value = false
                }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    confirmButton = {
                        Button(onClick = onConfirm) { Text("Yes") }
                    },
                    dismissButton = {
                        Button(onClick = onDismiss) { Text("Cancel") }
                    },
                    text = {
                        Text("Are you sure you want to delete the selected note(s)?")
                    }
                )
            }
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
        // If not already selected

        if (isNoteSelected[id] != true) {
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
    if (isSystemInDarkTheme()) {
        color = md_theme_dark_surfaceVariant
        contentColor = md_theme_dark_onSurfaceVariant
    } else {
        color = md_theme_light_surfaceVariant
        contentColor = md_theme_light_onSurfaceVariant
    }
    val offsetPadding = if (isSelected == true) 32.dp else 0.dp

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
            )
            .offset(x = offsetPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(note.id) },
                    onLongClick = { onLongClick(note.id) })
                .padding(Value.smallPadding),
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