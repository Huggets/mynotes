package com.huggets.mynotes.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.huggets.mynotes.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditing(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    noteId: Long,
    parentNoteId: Long,
    saveNote: (NoteItemUiState, Long) -> Unit,
    deleteNote: (noteId: Long) -> Unit,
) {
    var isDeleted by rememberSaveable { mutableStateOf(false) }
    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val showCancelConfirmation = rememberSaveable { mutableStateOf(false) }
    val showTitleEmptyDialog = rememberSaveable { mutableStateOf(false) }

    val isNewNote = noteId == 0L

    val onBackPressed = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelConfirmation.value = true
            }
        }
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Add a callback called when back is pressed
    // Remove it when leaving the composition
    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher?.onBackPressedDispatcher?.addCallback(
            lifecycleOwner,
            onBackPressed
        )

        onDispose {
            onBackPressed.remove()
        }
    }

    if (isDeleted) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else {
        val note = if (isNewNote) {
            NoteItemUiState(0, "", "", NoteItemUiState.getCurrentEditTime())
        } else {
            appState.value.allNotes.find(noteId)
                ?: throw NoSuchElementException("Note with id=$noteId not found")
        }

        val title = rememberSaveable { mutableStateOf(note.title) }
        val content = rememberSaveable { mutableStateOf(note.content) }
        val onSave: () -> Unit = {
            if (title.value.isBlank()) {
                showTitleEmptyDialog.value = true
            } else {
                saveNote(
                    NoteItemUiState(
                        note.id,
                        title.value,
                        content.value,
                        NoteItemUiState.getCurrentEditTime()
                    ), parentNoteId
                )
                navigationController.popBackStack()
            }
        }
        val onDelete: () -> Unit = {
            showDeleteConfirmation.value = true
        }
        val onBack: () -> Unit = {
            showCancelConfirmation.value = true
        }

        ConfirmationDialog(
            displayDialog = showDeleteConfirmation,
            onConfirmation = {
                if (!isNewNote) {
                    deleteNote(noteId)
                    isDeleted = true
                }
                navigationController.popBackStack()
            },
            confirmationMessage = "Are you sure you want to delete this note?"
        )
        ConfirmationDialog(
            displayDialog = showCancelConfirmation,
            onConfirmation = {
                navigationController.popBackStack()
            },
            confirmationMessage = "Cancel changes?",
        )
        AlertDialog(
            displayDialog = showTitleEmptyDialog,
            message = "Title cannot be empty!",
        )

        Scaffold(
            topBar = { AppBar(onDelete = onDelete, onSave = onSave, onBack = onBack) },
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .padding(Value.smallPadding)
            ) {
                val index = rememberSaveable { mutableStateOf(0) }

                val editingVisibilityState = remember { MutableTransitionState(index.value == 0) }
                val associationVisibilityState =
                    remember { MutableTransitionState(index.value == 1) }

                val swipeDuration = 300
                val swipeInLeftTransition =
                    slideInHorizontally(Value.Animation.emphasized(swipeDuration)) { (it + it * Value.Animation.slideOffset).toInt() }
                val swipeInRightTransition =
                    slideInHorizontally(Value.Animation.emphasized(swipeDuration)) { -(it + it * Value.Animation.slideOffset).toInt() }
                val swipeOutLeftTransition =
                    slideOutHorizontally(Value.Animation.emphasized(swipeDuration)) { -(it + it * Value.Animation.slideOffset).toInt() }
                val swipeOutRightTransition =
                    slideOutHorizontally(Value.Animation.emphasized(swipeDuration)) { (it + it * Value.Animation.slideOffset).toInt() }

                Tab(
                    index = index,
                    editingVisibilityState = editingVisibilityState,
                    associationVisibilityState = associationVisibilityState,
                    swipeDuration = swipeDuration,
                )

                Box {
                    this@Column.AnimatedVisibility(
                        visibleState = editingVisibilityState,
                        enter = swipeInRightTransition,
                        exit = swipeOutLeftTransition,
                    ) {
                        Editing(title, content)
                    }
                    this@Column.AnimatedVisibility(
                        visibleState = associationVisibilityState,
                        enter = swipeInLeftTransition,
                        exit = swipeOutRightTransition,
                    ) {
                        val associatedNotes: MutableList<NoteAssociationItemUiState> =
                            mutableListOf()

                        appState.value.noteAssociations.forEach {
                            if (it.parentId == noteId) {
                                associatedNotes += it
                            }
                        }

                        AssociatedNotes(
                            parentNoteId = noteId,
                            associatedNotes = associatedNotes,
                            notes = appState.value.allNotes,
                            navigationController = navigationController,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIndicator(
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            content = {},
        )
    }
}

@Composable
private fun Tab(
    index: MutableState<Int>,
    editingVisibilityState: MutableTransitionState<Boolean>,
    associationVisibilityState: MutableTransitionState<Boolean>,
    swipeDuration: Int,
    modifier: Modifier = Modifier,
) {

    TabRow(
        selectedTabIndex = index.value,
        modifier = modifier.padding(0.dp, 0.dp, 0.dp, 8.dp),
        indicator = { tabPositions ->
            val transition = updateTransition(index.value, label = "tabSwitch")
            val indicatorStart by transition.animateDp(
                { Value.Animation.emphasized(swipeDuration) }, label = "tabSwitchStart"
            ) {
                tabPositions[it].left
            }

            val indicatorEnd by transition.animateDp(
                { Value.Animation.emphasized(swipeDuration) }, label = "tabSwitchEnd"
            ) {
                tabPositions[it].right
            }

            TabIndicator(
                Modifier
                    .wrapContentSize(align = Alignment.BottomStart)
                    .offset(x = indicatorStart)
                    .width(indicatorEnd - indicatorStart)
            )
        }
    ) {
        Tab(
            selected = index.value == 0,
            onClick = {
                index.value = 0
                editingVisibilityState.targetState = true
                associationVisibilityState.targetState = false
            },
            text = { Text("Edit") })
        Tab(
            selected = index.value == 1,
            onClick = {
                index.value = 1
                editingVisibilityState.targetState = false
                associationVisibilityState.targetState = true
            },
            text = { Text("View associated notes") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Editing(
    title: MutableState<String>,
    content: MutableState<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        OutlinedTextField(
            value = title.value,
            onValueChange = { title.value = it },
            singleLine = true,
            label = {
                Text("Title")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = content.value,
            onValueChange = { content.value = it },
            label = {
                Text("Content")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AssociatedNotes(
    parentNoteId: Long,
    associatedNotes: List<NoteAssociationItemUiState>,
    notes: List<NoteItemUiState>,
    navigationController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val parentExists = parentNoteId != 0L

    LazyColumn(modifier) {
        item(0) {
            Button(
                onClick = {
                    // Create a new note
                    navigationController.navigate(
                        Destinations.generateEditNoteDestination(parentId = parentNoteId)
                    )
                },
                shape = ShapeDefaults.Small,
                modifier = Modifier
                    .padding(0.dp, 0.dp, 0.dp, 8.dp)
                    .fillMaxWidth(),
                enabled = parentExists,
            ) {
                if (parentExists) {
                    Text("Associate note a new note")
                } else {
                    Text("You need to save before adding associated notes")
                }
            }
        }
        for (associatedNote in associatedNotes) {
            val note = notes.find(associatedNote.childId).let {
                it ?: throw NoSuchElementException(
                    "Note with id=${associatedNote.childId} not found"
                )
            }

            item(associatedNote.childId) {
                AssociatedNoteElement(
                    text = note.title,
                    onClick = {
                        // Open the note
                        navigationController.navigate(
                            Destinations.generateEditNoteDestination(note.id)
                        )
                    },
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 0.dp, 8.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AssociatedNoteElement(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = ShapeDefaults.Small,
        modifier = modifier.clickable { onClick() },
    ) {
        Row(modifier = Modifier.padding(Value.smallPadding)) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text("Edit note") },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete note")
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Done, "Save note")
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Cancel note")
            }
        },
        modifier = modifier,
    )
}