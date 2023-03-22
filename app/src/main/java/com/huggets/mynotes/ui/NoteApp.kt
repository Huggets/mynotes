package com.huggets.mynotes.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.note.NoteViewModel
import com.huggets.mynotes.theme.AppTheme

@Composable
fun NoteApp(
    changeOnBackPressedCallback: (() -> Unit) -> Unit,
    quitApplication: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(),
) {
    val navigationController = rememberNavController()
    val appState = noteViewModel.uiState.collectAsStateWithLifecycle()

    noteViewModel.fetchNotes()

    AppTheme {
        Surface {
            NavHost(
                navController = navigationController,
                startDestination = Destinations.generateViewNoteList(),
            ) {
                composable(Destinations.viewNoteListRoute) {
                    ViewNoteList(
                        changeOnBackPressedCallback,
                        quitApplication,
                        navigationController,
                        appState
                    )
                }
                composable(Destinations.editNoteRoute) { backStackEntry ->
                    val isNewNote =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.isNewNote)!!
                            .toBoolean()
                    val noteId =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.noteId)!!
                            .toInt()
                    val saveNote: (NoteItemUiState) -> Unit = {
                        noteViewModel.saveNote(it)
                        noteViewModel.fetchNotes()
                    }
                    val deleteNote: (Int) -> Unit = {
                        noteViewModel.deleteNote(it)
                        noteViewModel.fetchNotes()
                    }

                    EditNote(
                        changeOnBackPressedCallback,
                        navigationController,
                        appState,
                        noteId,
                        isNewNote,
                        saveNote,
                        deleteNote
                    )
                }
            }
        }
    }
}
