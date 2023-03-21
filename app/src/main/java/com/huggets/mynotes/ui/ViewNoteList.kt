package com.huggets.mynotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huggets.mynotes.*
import com.huggets.mynotes.note.NoteAppUiState
import com.huggets.mynotes.note.NoteItemUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNoteList(
    changeOnBackPressedCallback: (() -> Unit) -> Unit,
    quitApplication: () -> Unit,
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
) {
    // Navigate back or quit the application if the back stack is empty
    val onBackPressed: () -> Unit = {
        val navigationFailed = !navigationController.navigateUp()
        if (navigationFailed) {
            quitApplication()
        }
    }
    changeOnBackPressedCallback(onBackPressed)

    Scaffold(
        topBar = { ViewNoteListAppBar() },
        floatingActionButton = { ViewNoteListFab(navigationController) },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        NoteList(navigationController, appState, Modifier.padding(padding))
    }
}

@Composable
private fun NoteList(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    modifier: Modifier = Modifier
) {
    if (appState.value.items.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth()) {
            Text(text = "No notes", fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            for (note in appState.value.items) {
                item(key = note.id) {
                    NoteElement(navigationController, note)
                }
            }
        }
    }
}

@Composable
private fun NoteElement(
    navigationController: NavHostController,
    note: NoteItemUiState,
    modifier: Modifier = Modifier
) {
    val openNote: () -> Unit = {
        navigationController.navigate(Destinations.generateEditNoteDestination(false, note.id))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = openNote)
            .padding(smallPadding)
    ) {
        val title = note.title.let {
            if (it.isBlank()) "No title"
            else shortened(it, 20)
        }
        val content = shortened(note.content, 120)

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = content,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewNoteListAppBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text("View notes") },
        modifier = modifier,
    )
}

@Composable
private fun ViewNoteListFab(navigationController: NavHostController) {
    val openNewNote: () -> Unit = {
        navigationController.navigate(
            Destinations.generateEditNoteDestination(true)
        )
    }

    FloatingActionButton(onClick = openNewNote) {
        Icon(Icons.Filled.Add, "Add a new note")
    }
}