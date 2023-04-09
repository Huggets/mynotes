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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.huggets.mynotes.note.NoteViewModel
import com.huggets.mynotes.theme.AppTheme
import com.huggets.mynotes.ui.Value.Animation
import com.huggets.mynotes.ui.Value.Animation.slideOffset

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
    exportToXml: () -> Unit,
    importFromXml: () -> Unit,
    noteViewModel: NoteViewModel,
) {
    val navigationController = rememberAnimatedNavController()
    val appState = noteViewModel.uiState.collectAsStateWithLifecycle()
    val fabPosition =
        rememberSaveable(stateSaver = fabPositionSaver) { mutableStateOf(FabPosition.Center) }

    val enterScreenSpec = Animation.emphasizedDecelerate<IntOffset>()
    val exitScreenPermanentlySpec = Animation.emphasizedAccelerate<IntOffset>()
    val fadeInSpec = tween<Float>(enterScreenSpec.durationMillis)
    val fadeOutSpec = tween<Float>(exitScreenPermanentlySpec.durationMillis)

    val enterScreenFromRight =
        (fadeIn(fadeInSpec) + slideInHorizontally(enterScreenSpec) { (it * slideOffset).toInt() })
    val enterScreenFromLeft =
        (fadeIn(fadeInSpec) + slideInHorizontally(enterScreenSpec) { -(it * slideOffset).toInt() })
    val leaveScreenToRight =
        (fadeOut(fadeOutSpec) + slideOutHorizontally(exitScreenPermanentlySpec) { (it * slideOffset).toInt() })
    val leaveScreenToLeft =
        (fadeOut(fadeOutSpec) + slideOutHorizontally(exitScreenPermanentlySpec) { -(it * slideOffset).toInt() })
    val inNewNoteCenter =
        (scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)) +
                slideIn { IntOffset(0, it.height) })
    val inNewNoteRight =
        (scaleIn(
            transformOrigin = TransformOrigin(1f, 1f)
        ) + slideIn { IntOffset(it.width, it.height) })
    val outNewNoteCenter =
        (scaleOut(
            transformOrigin = TransformOrigin(0.5f, 1f)
        ) + slideOut { IntOffset(0, it.height) })
    val outNewNoteRight =
        (scaleOut(
            transformOrigin = TransformOrigin(1f, 1f)
        ) + slideOut { IntOffset(it.width, it.height) })

    val isNewNote: (NavBackStackEntry) -> Boolean = {
        it.arguments?.getString(Destinations.ParametersName.noteId)?.toLong() == 0L
    }

    noteViewModel.syncUiState()

    AppTheme {
        Surface {
            AnimatedNavHost(
                navController = navigationController,
                startDestination = Destinations.generateViewNoteList(),
            ) {
                composable(
                    Destinations.viewNoteListRoute,
                    popEnterTransition = {
                        if (isNewNote(initialState)) {
                            null
                        } else {
                            enterScreenFromLeft
                        }
                    },
                    exitTransition = {
                        if (isNewNote(targetState)) {
                            null
                        } else {
                            leaveScreenToLeft
                        }
                    },
                ) {
                    val deleteNotes: (List<Long>) -> Unit = { noteIds ->
                        for (id in noteIds) {
                            noteViewModel.deleteNote(id)
                        }
                    }
                    NoteList(
                        quitApplication,
                        navigationController,
                        appState,
                        fabPosition,
                        deleteNotes,
                        exportToXml,
                        importFromXml,
                    )
                }
                composable(
                    Destinations.editNoteRoute,
                    enterTransition = {
                        if (this.initialState.destination.route == Destinations.viewNoteListRoute &&
                            isNewNote(targetState)
                        ) {
                            if (fabPosition.value == FabPosition.Center) {
                                inNewNoteCenter
                            } else {
                                inNewNoteRight
                            }

                        } else {
                            enterScreenFromRight
                        }
                    },
                    popExitTransition = {
                        if (targetState.destination.route == Destinations.viewNoteListRoute &&
                            isNewNote(initialState)
                        ) {
                            if (fabPosition.value == FabPosition.Center) {
                                outNewNoteCenter
                            } else {
                                outNewNoteRight
                            }
                        } else {
                            leaveScreenToRight
                        }
                    },
                    popEnterTransition = {
                        enterScreenFromLeft
                    },
                    exitTransition = {
                        leaveScreenToLeft
                    },
                ) { backStackEntry ->
                    val noteId =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.noteId)!!
                            .toLong()
                    val parentNoteId =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.parentNoteId)!!
                            .toLong()
                    val saveNote: (NoteItemUiState, Long) -> Unit = { note, parentId ->
                        noteViewModel.saveNote(note, parentId)
                    }
                    val deleteNote: (Long) -> Unit = {
                        noteViewModel.deleteNote(it)
                    }

                    NoteEditing(
                        navigationController,
                        appState,
                        noteId,
                        parentNoteId,
                        saveNote,
                        deleteNote,
                    )
                }
            }
        }
    }
}