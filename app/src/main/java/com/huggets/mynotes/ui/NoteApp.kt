package com.huggets.mynotes.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.huggets.mynotes.note.NoteItemUiState
import com.huggets.mynotes.note.NoteViewModel
import com.huggets.mynotes.theme.AppTheme
import com.huggets.mynotes.ui.Value.Animation

@OptIn(ExperimentalMaterial3Api::class)
private val fabPositionSaver = object : Saver<FabPosition, Boolean> {

    override fun restore(value: Boolean): FabPosition {
        return if (value) FabPosition.Center else FabPosition.End
    }

    override fun SaverScope.save(value: FabPosition): Boolean {
        return value == FabPosition.Center
    }

}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteApp(
    quitApplication: () -> Unit,
    noteViewModel: NoteViewModel,
) {
    val navigationController = rememberAnimatedNavController()
    val appState = noteViewModel.uiState.collectAsStateWithLifecycle()
    val fabPosition =
        rememberSaveable(stateSaver = fabPositionSaver) { mutableStateOf(FabPosition.Center) }

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
                        if (this.initialState.destination.route == Destinations.editNoteRoute) {
                            val newNote =
                                this.initialState.arguments?.getString(Destinations.ParametersName.noteId)
                                    ?.toLong() == 0L

                            if (newNote) {
                                null
                            } else {
                                fadeIn(fadeInSpec) + slideInHorizontally(enterScreen) { -(it * slideOffset).toInt() }
                            }
                        } else {
                            null
                        }


                    },
                    exitTransition = {
                        if (this.targetState.destination.route == Destinations.editNoteRoute) {
                            val newNote =
                                this.targetState.arguments?.getString(Destinations.ParametersName.noteId)
                                    ?.toLong() == 0L

                            if (newNote) {
                                null
                            } else {
                                fadeOut(fadeOutSpec) + slideOutHorizontally(exitScreenPermanently) { -(it * slideOffset).toInt() }
                            }
                        } else {
                            null
                        }
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
                        fabPosition,
                        deleteNotes,
                    )
                }
                composable(
                    Destinations.editNoteRoute,
                    enterTransition = {
                        val newNote =
                            this.targetState.arguments?.getString(Destinations.ParametersName.noteId)
                                ?.toLong() == 0L

                        if (newNote) {
                            slideIn(enterScreen) {
                                if (fabPosition.value == FabPosition.Center) {
                                    IntOffset(it.width / 2, it.height)
                                } else {
                                    IntOffset(it.width, it.height)
                                }
                            } + scaleIn(fadeInSpec)
                        } else {
                            fadeIn(fadeInSpec) + slideInHorizontally(enterScreen) { (it * slideOffset).toInt() }
                        }
                    },
                    exitTransition = {
                        val newNote =
                            this.initialState.arguments?.getString(Destinations.ParametersName.noteId)
                                ?.toLong() == 0L

                        if (newNote) {
                            slideOut(exitScreenPermanently) {
                                if (fabPosition.value == FabPosition.Center) {
                                    IntOffset(it.width / 2, it.height)
                                } else {
                                    IntOffset(it.width, it.height)
                                }
                            } + scaleOut(fadeOutSpec)
                        } else {
                            fadeOut(fadeOutSpec) + slideOutHorizontally(exitScreenPermanently) { (it * slideOffset).toInt() }
                        }
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
