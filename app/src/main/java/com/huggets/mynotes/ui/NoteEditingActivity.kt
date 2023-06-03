package com.huggets.mynotes.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huggets.mynotes.R
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.theme.*
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteAssociationItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState.Companion.find

/**
 * The UI for the note editing activity.
 *
 * @param appState The app state.
 * @param noteCreationDate The creation date of the note to edit.
 * @param isNew Whether the note is new or not.
 * @param navigateUp The lambda to call to navigate up.
 * @param navigateToNote The lambda to call to navigate to another note.
 * @param createNote The lambda to call to create a new note.
 * @param saveNote The lambda to call to save a note.
 * @param deleteNote The lambda to call to delete a note.
 */
@Composable
fun NoteEditingActivity(
    appState: State<NoteAppUiState>,
    noteCreationDate: Date,
    isNew: Boolean,
    navigateUp: () -> Unit,
    navigateToNote: (creationDate: Date, isNew: Boolean) -> Unit,
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit,
    saveNote: (note: NoteItemUiState, onNoteUpdated: () -> Unit) -> Unit,
    deleteNote: (noteCreationDate: Date) -> Unit,
) {
    var isDeleted by rememberSaveable { mutableStateOf(false) }
    var isModified by rememberSaveable { mutableStateOf(false) }

    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val showCancelConfirmation = rememberSaveable { mutableStateOf(false) }

    val cancelChangesAndNavigateUp = {
        if (isModified) {
            showCancelConfirmation.value = true
        } else {
            if (isNew) {
                // The note is not kept and must be deleted
                isDeleted = true
                deleteNote(noteCreationDate)
            }

            navigateUp()
        }
    }

    BackPressHandler(cancelChangesAndNavigateUp)

    ConfirmationDialog(
        displayDialog = showDeleteConfirmation,
        onConfirm = {
            deleteNote(noteCreationDate)
            isDeleted = true
            navigateUp()
        },
        message = stringResource(R.string.confirmation_message_delete_note),
    )
    ConfirmationDialog(
        displayDialog = showCancelConfirmation,
        onConfirm = {
            if (isNew) {
                // The note is not kept and must be deleted
                deleteNote(noteCreationDate)
                isDeleted = true
            }
            navigateUp()
        },
        message = stringResource(R.string.confirmation_message_cancel_changes),
    )

    // Note can be null if it was just created (the view model has not finished updating yet)
    val note = appState.value.allNotes.find(noteCreationDate)

    if (isDeleted || note == null) {
        Box(Values.Modifier.maxSize) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else {
        var title by rememberSaveable { mutableStateOf(note.title) }
        var content by rememberSaveable { mutableStateOf(note.content) }

        val saveChanges: () -> Unit = {
            val now = Date.getCurrentTime()
            val updatedNote = NoteItemUiState(title, content, note.creationDate, now)

            saveNote(updatedNote) {
                isModified = false
            }
        }
        val showDeleteConfirmationDialog: () -> Unit = {
            showDeleteConfirmation.value = true
        }

        val textFieldColors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )

        Scaffold(
            topBar = {
                AppBar(
                    titleProvider = { title },
                    saveIconVisibleStateProvider = { isModified },
                    textFieldColors = textFieldColors,
                    onDelete = showDeleteConfirmationDialog,
                    onSave = saveChanges,
                    onBack = cancelChangesAndNavigateUp,
                    onTitleModified = {
                        title = it
                        isModified = true
                    },
                )
            },
        ) { paddingValues ->
            MainContent(
                appState = appState,
                note = note,
                textFieldColors = textFieldColors,
                modifier = Modifier
                    .padding(paddingValues)
                    .then(Values.Modifier.maxWidth),
                contentProvider = { content },
                onContentChanges = {
                    content = it
                    isModified = true
                },
                navigateToNote = navigateToNote,
                createNote = createNote,
            )
        }
    }
}

/**
 * The main content of the note editing activity.
 *
 * @param appState The app state.
 * @param note The note to edit.
 * @param modifier The modifier to apply to the content.
 * @param textFieldColors The colors to use for the text fields.
 * @param contentProvider The lambda to call to get the content of the note.
 * @param onContentChanges The lambda to call when the content of the note changes.
 * @param navigateToNote The lambda to call to navigate to another note.
 * @param createNote The lambda to call to create a new note.
 */
@Composable
private fun MainContent(
    appState: State<NoteAppUiState>,
    note: NoteItemUiState,
    modifier: Modifier = Modifier,
    textFieldColors: TextFieldColors = TextFieldDefaults.colors(),
    contentProvider: () -> String = { "" },
    onContentChanges: (String) -> Unit = {},
    navigateToNote: (creationDate: Date, isNew: Boolean) -> Unit = { _, _ -> },
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit = { _, _ -> },
) {
    Column(modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }

        var editingVisibilityState by remember { mutableStateOf(tabIndex == 0) }
        var associationVisibilityState by remember { mutableStateOf(tabIndex == 1) }

        Tabs(
            indexProvider = { tabIndex },
            onEditClicked = {
                tabIndex = 0
                editingVisibilityState = true
                associationVisibilityState = false
            },
            onAssociationClicked = {
                tabIndex = 1
                editingVisibilityState = false
                associationVisibilityState = true
            },
            modifier = Modifier.padding(Values.smallPadding, 0.dp),
        )

        Box {
            ContentEditor(
                visibleStateProvider = { editingVisibilityState },
                contentProvider = contentProvider,
                onContentChanges = onContentChanges,
                textFieldColors = textFieldColors,
            )

            AssociatedNotes(
                appState = appState,
                note = note,
                visibleStateProvider = { associationVisibilityState },
                createNote = createNote,
                navigateToNote = navigateToNote,
            )
        }
    }
}

/**
 * The tabs to switch between the note editing and the associated notes.
 *
 * @param indexProvider The lambda to call to get the index of the selected tab.
 * @param modifier The modifier to apply to this layout.
 * @param onEditClicked The lambda to call when the edit tab is clicked.
 * @param onAssociationClicked The lambda to call when the associated notes tab is clicked.
 */
@Composable
private fun Tabs(
    indexProvider: () -> Int,
    modifier: Modifier = Modifier,
    onEditClicked: () -> Unit = {},
    onAssociationClicked: () -> Unit = {},
) {
    val index = indexProvider()

    TabRow(
        selectedTabIndex = index,
        modifier = modifier,
        indicator = { TabIndicator(index = index, tabPositions = it) },
    ) {
        Tab(
            selected = index == 0,
            onClick = onEditClicked,
            text = { Text(stringResource(R.string.tab_edit)) },
        )
        Tab(
            selected = index == 1,
            onClick = onAssociationClicked,
            text = { Text(stringResource(R.string.tab_associated_notes)) },
        )
    }
}

/**
 * The indicator of the [Tabs].
 *
 * @param index The index of the selected tab.
 * @param tabPositions The positions of the tabs.
 */
@Composable
private fun TabIndicator(
    index: Int,
    tabPositions: List<TabPosition>
) {
    val transition = updateTransition(targetState = index, label = "tabSwitch")
    val indicatorStart by transition.animateDp(
        transitionSpec = { Values.Animation.emphasized(swipeDuration) },
        label = "tabSwitchStart",
    ) {
        tabPositions[it].left
    }
    val indicatorEnd by transition.animateDp(
        transitionSpec = { Values.Animation.emphasized(swipeDuration) },
        label = "tabSwitchEnd",
    ) {
        tabPositions[it].right
    }

    Box(
        Modifier
            .wrapContentSize(Alignment.BottomStart)
            .offset {
                IntOffset(indicatorStart.roundToPx(), 0)
            }
            .width(indicatorEnd - indicatorStart)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Values.Modifier.maxWidth
                .height(2.dp),
            content = {},
        )
    }
}

/**
 * The content editor.
 *
 * @param visibleStateProvider The lambda to call that returns whether the content editing is visible.
 * @param contentProvider The lambda to call to get the content of the note.
 * @param onContentChanges The lambda to call when the content of the note changes. It takes the new
 * content as a parameter.
 * @param textFieldColors The colors to use for the text field.
 */
@Composable
private fun ContentEditor(
    visibleStateProvider: () -> Boolean,
    contentProvider: () -> String = { "" },
    onContentChanges: (String) -> Unit = {},
    textFieldColors: TextFieldColors = TextFieldDefaults.colors(),
) {
    AnimatedVisibility(
        visible = visibleStateProvider(),
        enter = swipeInRightTransition,
        exit = swipeOutLeftTransition,
    ) {
        TextField(
            value = contentProvider(),
            onValueChange = onContentChanges,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Values.Modifier.maxSize,
        )
    }
}

/**
 * A list of the notes associated to the current note with a button to associate a new note.
 *
 * @param appState The state of the app.
 * @param note The parent note of the associated notes.
 * @param visibleStateProvider The lambda to call that returns whether the associated notes are
 * visible.
 * @param createNote The lambda to call when the user wants to create a new note. It takes the
 * parent creation date and a lambda to call when the creation is done as parameters.
 * @param navigateToNote The lambda to call when the user wants to navigate to a note. It takes the
 * creation date of the note and whether the note is new as parameters.
 */
@Composable
private fun AssociatedNotes(
    appState: State<NoteAppUiState>,
    note: NoteItemUiState,
    visibleStateProvider: () -> Boolean,
    createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit = { _, _ -> },
    navigateToNote: (creationDate: Date, isNew: Boolean) -> Unit = { _, _ -> },
) {
    AnimatedVisibility(
        visible = visibleStateProvider(),
        enter = swipeInLeftTransition,
        exit = swipeOutRightTransition,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Values.smallSpacing),
            modifier = Values.Modifier.smallPadding,
        ) {
            val associatedNotes: MutableList<NoteAssociationItemUiState> =
                mutableListOf()

            appState.value.noteAssociations.forEach {
                if (it.parentCreationDate == note.creationDate) {
                    associatedNotes += it
                }
            }

            item(0) {
                Button(
                    onClick = {
                        // Create and edit the new note
                        createNote(note.creationDate) { newNoteCreationDate ->
                            navigateToNote(newNoteCreationDate, true)
                        }
                    },
                    shape = ShapeDefaults.Small,
                    modifier = Values.Modifier.maxWidth,
                ) {
                    Text(stringResource(R.string.associate_new_note))
                }
            }
            for (associatedNote in associatedNotes) {
                appState.value.allNotes.find(associatedNote.childCreationDate)?.let { note ->
                    item(associatedNote.childCreationDate.hashCode()) {
                        AssociatedNoteElement(
                            title = note.title,
                            onClick = {
                                navigateToNote(note.creationDate, false)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single element of the associated notes list.
 *
 * @param title The title of the note.
 * @param onClick The action to perform when the element is clicked.
 */
@Composable
private fun AssociatedNoteElement(
    title: String,
    onClick: () -> Unit = {},
) {
    val density = LocalDensity.current
    var surfaceWidth by remember { mutableStateOf(0.dp) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = ShapeDefaults.Small,
        modifier = Values.Modifier.maxWidth.onGloballyPositioned {
            surfaceWidth = with(density) { it.size.width.toDp() }
        },
    ) {
        Row(modifier = Modifier.clickable(onClick = onClick)) {
            Text(
                text = title.ifBlank { stringResource(R.string.no_title) },
                fontWeight = FontWeight.Bold,
                fontSize = Values.normalFontSize,
                maxLines = 2,
                modifier = Values.Modifier.smallPadding,
            )
        }
    }
}

/**
 * The app bar for the note editing screen.
 *
 * @param titleProvider Provides the title of the note.
 * @param onDelete Called when the delete button is pressed.
 * @param onSave Called when the save button is pressed.
 * @param onBack Called when the back button is pressed.
 * @param onTitleModified Called when the title is modified. It takes the new title as a parameter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    titleProvider: () -> String,
    saveIconVisibleStateProvider: () -> Boolean,
    textFieldColors: TextFieldColors = TextFieldDefaults.colors(),
    onDelete: () -> Unit = {},
    onSave: () -> Unit = {},
    onBack: () -> Unit = {},
    onTitleModified: (String) -> Unit = {},
) {
    TopAppBar(
        title = {
            TextField(
                value = titleProvider(),
                onValueChange = onTitleModified,
                singleLine = true,
                colors = textFieldColors,
                placeholder = {
                    Text(stringResource(R.string.no_title), fontSize = 24.sp)
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Values.Modifier.maxWidth,
            )
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete_note))
            }
            AnimatedIconButton(
                visibleStateProvider = saveIconVisibleStateProvider,
                onClick = onSave,
            ) {
                Icon(Icons.Filled.Done, stringResource(R.string.save_note))
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, stringResource(R.string.cancel_changes))
            }
        },
    )
}

private const val swipeDuration = 300
private val swipeInLeftTransition =
    slideInHorizontally(Values.Animation.emphasized(swipeDuration)) { (it + it * Values.Animation.slideOffset).toInt() }
private val swipeInRightTransition =
    slideInHorizontally(Values.Animation.emphasized(swipeDuration)) { -(it + it * Values.Animation.slideOffset).toInt() }
private val swipeOutLeftTransition =
    slideOutHorizontally(Values.Animation.emphasized(swipeDuration)) { -(it + it * Values.Animation.slideOffset).toInt() }
private val swipeOutRightTransition =
    slideOutHorizontally(Values.Animation.emphasized(swipeDuration)) { (it + it * Values.Animation.slideOffset).toInt() }
