package com.huggets.mynotes.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.note.NoteViewModel
import com.huggets.mynotes.theme.AppTheme
import com.huggets.mynotes.ui.Value.Animation

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NoteApp(
    quitApplication: () -> Unit,
    noteViewModel: NoteViewModel,
) {
    val navigationController = rememberAnimatedNavController()
    val appState = noteViewModel.uiState.collectAsStateWithLifecycle()

    val slideOffset = 0.2f
    val enterScreen = Animation.enterScreen<IntOffset>()
    val exitScreenPermanently = Animation.exitScreenPermanently<IntOffset>()
    val fadeInSpec = tween<Float>(enterScreen.durationMillis)
    val fadeOutSpec = tween<Float>(exitScreenPermanently.durationMillis)

    noteViewModel.fetchNotes()

    AppTheme {
        Surface {
            AnimatedNavHost(
                navController = navigationController,
                startDestination = Destinations.generateViewNoteList(),
            ) {
                composable(
                    Destinations.viewNoteListRoute,
                    enterTransition = {
                        val transition =
                            if (this.initialState.destination.route == Destinations.editNoteRoute) {
                                fadeIn(fadeInSpec) + slideInHorizontally(enterScreen) { -(it * slideOffset).toInt() }
                            } else {
                                null
                            }

                        transition
                    },
                    exitTransition = {
                        val transition =
                            if (this.targetState.destination.route == Destinations.editNoteRoute) {
                                fadeOut(fadeOutSpec) + slideOutHorizontally(exitScreenPermanently) { -(it * slideOffset).toInt() }
                            } else {
                                null
                            }

                        transition
                    }
                ) {
                    val deleteNotes: (List<Long>) -> Unit = { noteIds ->
                        for (id in noteIds) {
                            noteViewModel.deleteNote(id)
                        }
                        noteViewModel.fetchNotes()
                    }
                    ViewNoteList(
                        quitApplication,
                        navigationController,
                        appState,
                        deleteNotes,
                    )
                }
                composable(
                    Destinations.editNoteRoute,
                    enterTransition = {
                        fadeIn(fadeInSpec) + slideInHorizontally(enterScreen) { (it * slideOffset).toInt() }
                    },
                    exitTransition = {
                        fadeOut(fadeOutSpec) + slideOutHorizontally(exitScreenPermanently) { (it * slideOffset).toInt() }
                    },
                ) { backStackEntry ->
                    val noteId =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.noteId)!!
                            .toLong()
                    val saveNote: (NoteItemUiState) -> Unit = {
                        noteViewModel.saveNote(it)
                        noteViewModel.fetchNotes()
                    }
                    val deleteNote: (Long) -> Unit = {
                        noteViewModel.deleteNote(it)
                        noteViewModel.fetchNotes()
                    }

                    EditNote(
                        navigationController,
                        appState,
                        noteId,
                        saveNote,
                        deleteNote,
                    )
                }
            }
        }
    }
}
