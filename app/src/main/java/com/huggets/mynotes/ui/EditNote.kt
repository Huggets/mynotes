package com.huggets.mynotes.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.navigation.NavHostController
import com.huggets.mynotes.note.NoteAppUiState
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.note.find
import com.huggets.mynotes.theme.*

/**
 * Edit a new note if newNote is true. Otherwise edit an existing one,
 * contained in notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNote(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    noteId: Long,
    saveNote: (NoteItemUiState) -> Unit,
    deleteNote: (noteId: Long) -> Unit,
) {
    var isDeleted by rememberSaveable { mutableStateOf(false) }
    val showDeleteConfirmation = rememberSaveable { mutableStateOf(false) }
    val showCancelConfirmation = rememberSaveable { mutableStateOf(false) }

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
        ConfirmationDialog(
            displayDialog = showDeleteConfirmation,
            onConfirmation = {
                if (!isNewNote) {
                    deleteNote(noteId)
                    isDeleted = true
                }
                navigationController.navigateUp()
            },
            confirmationMessage = "Are you sure you want to delete this note?"
        )
        ConfirmationDialog(
            displayDialog = showCancelConfirmation,
            onConfirmation = {
                navigationController.navigateUp()
            },
            confirmationMessage = "Cancel changes?",
        )

        val note = if (isNewNote) {
            NoteItemUiState(0, "", "")
        } else {
            appState.value.items.find(noteId)
                ?: throw Exception("Note with id=$noteId not found")
        }

        var title by rememberSaveable { mutableStateOf(note.title) }
        var content by rememberSaveable { mutableStateOf(note.content) }
        val onSave: () -> Unit = {
            saveNote(NoteItemUiState(note.id, title, content))
            navigationController.navigateUp()
        }
        val onDelete: () -> Unit = {
            showDeleteConfirmation.value = true
        }

        Scaffold(
            topBar = { EditNoteAppBar(onDelete = onDelete, onSave = onSave) },
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .padding(Value.smallPadding)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = {
                        Text("Title")
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = {
                        Text("Content")
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNoteAppBar(
    onDelete: () -> Unit,
    onSave: () -> Unit,
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
        modifier = modifier,
    )
}