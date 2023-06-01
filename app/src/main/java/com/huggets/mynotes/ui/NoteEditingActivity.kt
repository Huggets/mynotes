package com.huggets.mynotes.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.theme.*
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteAssociationItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import com.huggets.mynotes.ui.state.find

@Composable
fun NoteEditingActivity(
    appState: State<NoteAppUiState>,
    noteCreationDate: Date,
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit,
    saveNote: (NoteItemUiState) -> Unit,
    deleteNote: (noteCreationDate: Date) -> Unit,
    navigateUp: () -> Unit,
    navigateToNote: (creationDate: Date, isNew: Boolean) -> Unit,
    isNew: Boolean,
) {
    var isDeleted by rememberSaveable { mutableStateOf(false) }
    val isModified = rememberSaveable { mutableStateOf(false) }
    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val showCancelConfirmation = rememberSaveable { mutableStateOf(false) }

    val cancelNoteChanges: () -> Unit = {
        if (isModified.value) {
            showCancelConfirmation.value = true
        } else {
            if (isNew) {
                // The note is not kept and must be deleted
                deleteNote(noteCreationDate)
                isDeleted = true
            }
            navigateUp()
        }
    }

    BackPressHandler(cancelNoteChanges)

    val note = appState.value.allNotes.find(noteCreationDate)

    if (isDeleted || note == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else {
        val title = rememberSaveable { mutableStateOf(note.title) }
        val content = rememberSaveable { mutableStateOf(note.content) }
        val saveChanges: () -> Unit = {
            saveNote(
                NoteItemUiState(
                    title.value,
                    content.value,
                    note.creationDate,
                    Date.getCurrentTime(),
                )
            )
        }
        val saveAndPopBackStack: () -> Unit = {
            saveChanges()
            navigateUp()
        }
        val showDeleteConfirmationDialog: () -> Unit = {
            showDeleteConfirmation.value = true
        }

        ConfirmationDialog(
            displayDialog = showDeleteConfirmation,
            onConfirmation = {
                deleteNote(noteCreationDate)
                isDeleted = true
                navigateUp()
            },
            message = "Are you sure you want to delete this note?"
        )
        ConfirmationDialog(
            displayDialog = showCancelConfirmation,
            onConfirmation = {
                if (isNew) {
                    // The note is not kept and must be deleted
                    deleteNote(noteCreationDate)
                    isDeleted = true
                }
                navigateUp()
            },
            message = "Cancel changes?",
        )

        Scaffold(
            topBar = {
                AppBar(
                    onDelete = showDeleteConfirmationDialog,
                    onSave = saveAndPopBackStack,
                    onBack = cancelNoteChanges,
                    title = title,
                    isTitleModified = isModified,
                )
            },
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(),
            ) {
                val index = rememberSaveable { mutableStateOf(0) }

                val editingVisibilityState = remember { MutableTransitionState(index.value == 0) }
                val associationVisibilityState =
                    remember { MutableTransitionState(index.value == 1) }

                val swipeDuration = 300
                val swipeInLeftTransition =
                    slideInHorizontally(Values.Animation.emphasized(swipeDuration)) { (it + it * Values.Animation.slideOffset).toInt() }
                val swipeInRightTransition =
                    slideInHorizontally(Values.Animation.emphasized(swipeDuration)) { -(it + it * Values.Animation.slideOffset).toInt() }
                val swipeOutLeftTransition =
                    slideOutHorizontally(Values.Animation.emphasized(swipeDuration)) { -(it + it * Values.Animation.slideOffset).toInt() }
                val swipeOutRightTransition =
                    slideOutHorizontally(Values.Animation.emphasized(swipeDuration)) { (it + it * Values.Animation.slideOffset).toInt() }

                Tab(
                    index = index,
                    editingVisibilityState = editingVisibilityState,
                    associationVisibilityState = associationVisibilityState,
                    swipeDuration = swipeDuration,
                    modifier = Modifier.padding(Values.smallPadding, 0.dp),
                )

                Box {
                    this@Column.AnimatedVisibility(
                        visibleState = editingVisibilityState,
                        enter = swipeInRightTransition,
                        exit = swipeOutLeftTransition,
                    ) {
                        val colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        )

                        TextField(
                            value = content.value,
                            onValueChange = {
                                content.value = it
                                isModified.value = true
                            },
                            colors = colors,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    this@Column.AnimatedVisibility(
                        visibleState = associationVisibilityState,
                        enter = swipeInLeftTransition,
                        exit = swipeOutRightTransition,
                    ) {
                        val associatedNotes: MutableList<NoteAssociationItemUiState> =
                            mutableListOf()

                        appState.value.noteAssociations.forEach {
                            if (it.parentCreationDate == note.creationDate) {
                                associatedNotes += it
                            }
                        }

                        AssociatedNotes(
                            parentCreationDate = noteCreationDate,
                            associatedNotes = associatedNotes,
                            notes = appState.value.allNotes,
                            createNote = createNote,
                            navigateToNote = navigateToNote,
                            modifier = Modifier.padding(Values.smallPadding),
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
    @Suppress("SameParameterValue") swipeDuration: Int,
    modifier: Modifier = Modifier,
) {

    TabRow(
        selectedTabIndex = index.value,
        modifier = modifier,
        indicator = { tabPositions ->
            val transition = updateTransition(index.value, label = "tabSwitch")
            val indicatorStart by transition.animateDp(
                { Values.Animation.emphasized(swipeDuration) }, label = "tabSwitchStart"
            ) {
                tabPositions[it].left
            }

            val indicatorEnd by transition.animateDp(
                { Values.Animation.emphasized(swipeDuration) }, label = "tabSwitchEnd"
            ) {
                tabPositions[it].right
            }

            TabIndicator(
                Modifier
                    .wrapContentSize(align = Alignment.BottomStart)
                    .offset(x = indicatorStart)
                    .width(indicatorEnd - indicatorStart),
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
            text = { Text("Edit") },
        )
        Tab(
            selected = index.value == 1,
            onClick = {
                index.value = 1
                editingVisibilityState.targetState = false
                associationVisibilityState.targetState = true
            },
            text = { Text("View associated notes") },
        )
    }
}

@Composable
private fun AssociatedNotes(
    parentCreationDate: Date,
    associatedNotes: List<NoteAssociationItemUiState>,
    notes: List<NoteItemUiState>,
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit,
    navigateToNote: (creationDate: Date, isNew: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Values.smallSpacing),
        modifier = modifier,
    ) {
        item(0) {
            Button(
                onClick = {
                    // Create and edit the new note
                    createNote(parentCreationDate) { newNoteCreationDate ->
                        navigateToNote(newNoteCreationDate, true)
                    }
                },
                shape = ShapeDefaults.Small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Associate a new note")
            }
        }
        for (associatedNote in associatedNotes) {
            notes.find(associatedNote.childCreationDate)?.let { note ->
                item(associatedNote.childCreationDate.hashCode()) {
                    AssociatedNoteElement(
                        text = note.title,
                        onClick = {
                            navigateToNote(note.creationDate, false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
    val density = LocalDensity.current
    var surfaceWidth by remember { mutableStateOf(0.dp) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = ShapeDefaults.Small,
        modifier = modifier.onGloballyPositioned {
            surfaceWidth = with(density) { it.size.width.toDp() }
        },
    ) {
        Row(modifier = Modifier.clickable { onClick() }) {
            Text(
                text = text.ifBlank { "No title" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                modifier = Modifier.padding(Values.smallPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    title: MutableState<String>,
    isTitleModified: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            val colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )

            TextField(
                value = title.value,
                onValueChange = {
                    title.value = it
                    isTitleModified.value = true
                },
                singleLine = true,
                colors = colors,
                placeholder = {
                    Text("No title", fontSize = 24.sp)
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
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