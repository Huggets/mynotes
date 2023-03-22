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
import androidx.compose.ui.ExperimentalComposeUiApi
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditNote(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    noteId: Int,
    isNewNote: Boolean,
    saveNote: (NoteItemUiState) -> Unit,
    deleteNote: (noteId: Int) -> Unit,
) {
    var isDeleted by rememberSaveable { mutableStateOf(false) }
    var isDeleting by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }

    val onBackPressed = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelConfirmation = true
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
        if (isDeleting) {
            val onDismiss: () -> Unit = { isDeleting = false }
            val onConfirm: () -> Unit = {
                if (!isNewNote) {
                    deleteNote(noteId)
                    isDeleted = true
                }
                isDeleting = false
                navigationController.navigateUp()
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
                    Text("Are you sure you want to delete this note?")
                }
            )
        }

        if (showCancelConfirmation) {
            val onDismiss: () -> Unit = { showCancelConfirmation = false }
            val onConfirm: () -> Unit = {
                showCancelConfirmation = false
                navigationController.navigateUp()
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    Button(onClick = onConfirm) { Text("Yes") }
                },
                dismissButton = {
                    Button(onClick = onDismiss) { Text("No") }
                },
                text = {
                    Text("Cancel changes?")
                }
            )
        }

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
            isDeleting = true
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