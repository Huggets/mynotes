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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huggets.mynotes.theme.*
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteAssociationItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import com.huggets.mynotes.ui.state.find
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditing(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    noteCreationDate: String?,
    parentNoteCreationDate: String?,
    saveNote: (NoteItemUiState, String?) -> Unit,
    deleteNote: (String) -> Unit,
) {
    var isDeleted by rememberSaveable { mutableStateOf(false) }
    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val showCancelConfirmation = rememberSaveable { mutableStateOf(false) }

    val isNewNote = noteCreationDate == null

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
        val currentTime = NoteItemUiState.getCurrentEditTime()
        val note = if (isNewNote) {
            NoteItemUiState("", "", currentTime, currentTime)
        } else {
            appState.value.allNotes.find(noteCreationDate!!)
                ?: throw NoSuchElementException("Note with creationDate=$noteCreationDate not found")
        }

        val title = rememberSaveable { mutableStateOf(note.title) }
        val content = rememberSaveable { mutableStateOf(note.content) }
        val onSave: () -> Unit = {
            saveNote(
                NoteItemUiState(
                    title.value,
                    content.value,
                    note.creationDate,
                    NoteItemUiState.getCurrentEditTime()
                ), parentNoteCreationDate
            )
            navigationController.popBackStack()
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
                    deleteNote(note.creationDate)
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

        Scaffold(
            topBar = {
                AppBar(
                    onDelete = onDelete,
                    onSave = onSave,
                    onBack = onBack,
                    title = title,
                )
            },
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
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
                    modifier = Modifier.padding(Value.smallPadding, 0.dp),
                )

                Box {
                    this@Column.AnimatedVisibility(
                        visibleState = editingVisibilityState,
                        enter = swipeInRightTransition,
                        exit = swipeOutLeftTransition,
                    ) {
                        val colors = TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        )

                        TextField(
                            value = content.value,
                            onValueChange = { content.value = it },
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
                            navigationController = navigationController,
                            modifier = Modifier.padding(Value.smallPadding)
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

@Composable
private fun AssociatedNotes(
    parentCreationDate: String?,
    associatedNotes: List<NoteAssociationItemUiState>,
    notes: List<NoteItemUiState>,
    navigationController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val parentExists = parentCreationDate != null

    LazyColumn(modifier) {
        item(0) {
            Button(
                onClick = {
                    // Create a new note
                    navigationController.navigate(
                        Destinations.generateEditNoteDestination(null, parentCreationDate)
                    )
                },
                shape = ShapeDefaults.Small,
                modifier = Modifier
                    .padding(0.dp, 0.dp, 0.dp, 8.dp)
                    .fillMaxWidth(),
                enabled = parentExists,
            ) {
                if (parentExists) {
                    Text("Associate a new note")
                } else {
                    Text("You need to save before adding associated notes")
                }
            }
        }
        for (associatedNote in associatedNotes) {
            notes.find(associatedNote.childCreationDate)?.let { note ->
                item(associatedNote.childCreationDate) {
                    AssociatedNoteElement(
                        text = note.title,
                        onClick = {
                            // Open the note
                            navigationController.navigate(
                                Destinations.generateEditNoteDestination(note.creationDate, null)
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
        modifier = modifier
            .clickable { onClick() }
            .onGloballyPositioned {
                surfaceWidth = with(density) { it.size.width.toDp() }
            },
    ) {
        Row(modifier = Modifier.padding(Value.smallPadding)) {
            val fontSize = 16.sp

            Text(
                text = text.ifBlank { "No title" },
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                maxLines = 2,
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
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            val colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )

            TextField(
                value = title.value,
                onValueChange = { title.value = it },
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