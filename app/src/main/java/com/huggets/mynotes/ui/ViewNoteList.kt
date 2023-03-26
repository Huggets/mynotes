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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huggets.mynotes.*
import com.huggets.mynotes.note.NoteAppUiState
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.theme.*

private val exitScreen = Value.Animation.exitScreen<Float>()
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
fun ViewNoteList(
    quitApplication: () -> Unit,
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    fabPosition: MutableState<FabPosition>,
    deleteNotes: (List<Long>) -> Unit,
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
            topBar = { ViewNoteListAppBar(deleteSelectedNote, deleteIconTransitionState) },
            floatingActionButton = {
                ViewNoteListFab(navigationController, this, fabTransitionState)
            },
            floatingActionButtonPosition = fabPosition.value,
        ) { padding ->
            NoteList(
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
private fun NoteList(
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

    if (appState.value.items.isEmpty()) {
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
            for (note in appState.value.items) {
                item(key = note.id) {
                    NoteElement(
                        note,
                        isNoteSelected[note.id],
                        onClick,
                        onLongClick,
                    )
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
        modifier = modifier.padding(Value.smallPadding, 0.dp),
    ) {
        var boxWidth by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onClick(note.id) },
                    onLongClick = { onLongClick(note.id) })
                .onGloballyPositioned {
                    boxWidth = with(density) { it.size.width.toDp() }
                },
        ) {
            Column(
                modifier = Modifier.padding(Value.smallPadding)
            ) {
                val title = note.title.let {
                    if (it.isBlank()) "No title"
                    else shortened(it, 1f, density, boxWidth, 16.sp)
                }
                val content = shortened(note.content, 5f, density, boxWidth, 16.sp)

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
            Surface(color = selectionColor, modifier = Modifier
                .matchParentSize()
                .alpha(
                    if (isSelected == true) 0.5f
                    else 0f
                ), content = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewNoteListAppBar(
    deleteSelectedNote: () -> Unit,
    deleteIconState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text("View notes") },
        actions = {
            AnimatedVisibility(
                visibleState = deleteIconState,
                enter = fadeIn(exitScreen),
                exit = fadeOut(exitScreen),
            ) {
                IconButton(onClick = deleteSelectedNote) {
                    Icon(Icons.Filled.Delete, "Delete selected notes")
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ViewNoteListFab(
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
        enter = scaleIn(exitScreen),
        exit = scaleOut(exitScreen),
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