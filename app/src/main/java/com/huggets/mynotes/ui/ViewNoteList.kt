package com.huggets.mynotes.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.huggets.mynotes.*
import com.huggets.mynotes.note.NoteAppUiState
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.theme.*

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

    BoxWithConstraints {
        Log.d("test", this.maxWidth.toString())
        val fabPosition =
            if (this.maxWidth < Value.Fab.minWidthRequiredFabToLeft) FabPosition.Center
            else FabPosition.End

        Scaffold(
            topBar = { ViewNoteListAppBar() },
            floatingActionButton = { ViewNoteListFab(navigationController, this) },
            floatingActionButtonPosition = fabPosition,
        ) { padding ->
            NoteList(navigationController, appState, Modifier.padding(padding))
        }
    }
}

@Composable
private fun NoteList(
    navigationController: NavHostController,
    appState: State<NoteAppUiState>,
    modifier: Modifier = Modifier
) {
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

    val color: Color
    val contentColor: Color
    if (isSystemInDarkTheme()) {
        color = md_theme_dark_surfaceVariant
        contentColor = md_theme_dark_onSurfaceVariant
    } else {
        color = md_theme_light_surfaceVariant
        contentColor = md_theme_light_onSurfaceVariant
    }

    Surface(
        color = color,
        contentColor = contentColor,
        shape = ShapeDefaults.Small,
        modifier = modifier.padding(Value.smallPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = openNote)
                .padding(Value.smallPadding)
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
                fontSize = 16.sp
            )
        }
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

    if (constraintsScope.maxWidth < Value.Fab.minWidthRequiredExtendedFab) {
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