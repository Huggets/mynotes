package com.huggets.mynotes.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.SnackbarHostState
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
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.NoteViewModel
import com.huggets.mynotes.theme.AppTheme
import com.huggets.mynotes.ui.Value.Animation
import com.huggets.mynotes.ui.Value.Animation.slideOffset
import com.huggets.mynotes.ui.state.NoteItemUiState

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
    snackbarHostState: SnackbarHostState,
) {
    val navigationController = rememberAnimatedNavController()
    val appState = noteViewModel.uiState.collectAsStateWithLifecycle()
    val fabPosition =
        rememberSaveable(stateSaver = fabPositionSaver) { mutableStateOf(FabPosition.Center) }

    val enterScreenIntOffsetSpec = Animation.emphasizedDecelerate<IntOffset>()
    val enterScreenFloatSpec = tween<Float>(enterScreenIntOffsetSpec.durationMillis)
    val exitScreenPermanentlyIntOffsetSpec = Animation.emphasizedAccelerate<IntOffset>()
    val exitScreenPermanentlyFloatSpec =
        tween<Float>(exitScreenPermanentlyIntOffsetSpec.durationMillis)

    val enterScreenFromRight =
        (fadeIn(enterScreenFloatSpec) +
                slideInHorizontally(enterScreenIntOffsetSpec) { (it * slideOffset).toInt() })
    val enterScreenFromLeft =
        (fadeIn(enterScreenFloatSpec) +
                slideInHorizontally(enterScreenIntOffsetSpec) { -(it * slideOffset).toInt() })
    val leaveScreenToRight =
        (fadeOut(exitScreenPermanentlyFloatSpec) +
                slideOutHorizontally(exitScreenPermanentlyIntOffsetSpec) {
                    (it * slideOffset).toInt()
                })
    val leaveScreenToLeft =
        (fadeOut(exitScreenPermanentlyFloatSpec) +
                slideOutHorizontally(exitScreenPermanentlyIntOffsetSpec) {
                    -(it * slideOffset).toInt()
                })
    val inNewNoteCenter =
        (scaleIn(
            transformOrigin = TransformOrigin(0.5f, 1f),
            animationSpec = enterScreenFloatSpec,
        ) +
                slideIn(enterScreenIntOffsetSpec) { IntOffset(0, it.height) })
    val inNewNoteRight =
        (scaleIn(
            transformOrigin = TransformOrigin(1f, 1f),
            animationSpec = enterScreenFloatSpec,
        ) +
                slideIn(enterScreenIntOffsetSpec) { IntOffset(it.width, it.height) })
    val outNewNoteCenter =
        (scaleOut(
            transformOrigin = TransformOrigin(0.5f, 1f),
            animationSpec = exitScreenPermanentlyFloatSpec,
        ) +
                slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(0, it.height) })
    val outNewNoteRight =
        (scaleOut(
            transformOrigin = TransformOrigin(1f, 1f),
            animationSpec = exitScreenPermanentlyFloatSpec,
        ) +
                slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(it.width, it.height) })

    val isNewNote: (NavBackStackEntry) -> Boolean = {
        it.destination.route == Destinations.newNoteRoute
    }

    val viewListPopEnterTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
        {
            if (isNewNote(initialState)) {
                null
            } else {
                enterScreenFromLeft
            }
        }
    val viewListExitTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
        {
            if (isNewNote(targetState)) {
                null
            } else {
                leaveScreenToLeft
            }
        }

    val editNoteEnterTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
        {
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
        }
    val editNoteExitTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
        {
            leaveScreenToLeft
        }
    val editNotePopEnterTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
        {
            enterScreenFromLeft
        }
    val editNotePopExitTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
        {
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
        }

    val createNote: (Date, Date?) -> Unit = { creationDate, parentCreationDate ->
        noteViewModel.createNote(creationDate, parentCreationDate)
    }
    val updateNote: (NoteItemUiState, Date?) -> Unit = { note, _ ->
        noteViewModel.updateNote(note)
    }
    val deleteNote: (Date) -> Unit = {
        noteViewModel.deleteNote(it)
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
                    popEnterTransition = viewListPopEnterTransition,
                    exitTransition = viewListExitTransition,
                ) {
                    val deleteNotes: (List<Date>) -> Unit = { noteCreationDates ->
                        for (creationDate in noteCreationDates) {
                            noteViewModel.deleteNote(creationDate)
                        }
                    }
                    NoteList(
                        quitApplication,
                        navigationController,
                        appState,
                        fabPosition,
                        deleteNotes,
                        createNote,
                        exportToXml,
                        importFromXml,
                        snackbarHostState,
                    )
                }
                composable(
                    Destinations.editNoteRoute,
                    enterTransition = editNoteEnterTransition,
                    exitTransition = editNoteExitTransition,
                    popExitTransition = editNotePopExitTransition,
                    popEnterTransition = editNotePopEnterTransition,
                ) { backStackEntry ->
                    val noteCreationDate = Date.fromString(
                        backStackEntry.arguments?.getString(Destinations.ParametersName.noteCreationDate)!!
                    )
                    val parentCreationDate =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.parentNoteCreationDate)
                            ?.let { Date.fromString(it) }

                    NoteEditing(
                        navigationController,
                        appState,
                        noteCreationDate,
                        parentCreationDate,
                        createNote,
                        updateNote,
                        deleteNote,
                        false,
                    )
                }
                composable(
                    Destinations.newNoteRoute,
                    enterTransition = editNoteEnterTransition,
                    exitTransition = editNoteExitTransition,
                    popExitTransition = editNotePopExitTransition,
                    popEnterTransition = editNotePopEnterTransition,
                ) { backStackEntry ->
                    val noteCreationDate = Date.fromString(
                        backStackEntry.arguments?.getString(Destinations.ParametersName.noteCreationDate)!!
                    )
                    val parentCreationDate =
                        backStackEntry.arguments?.getString(Destinations.ParametersName.parentNoteCreationDate)
                            ?.let { Date.fromString(it) }

                    NoteEditing(
                        navigationController,
                        appState,
                        noteCreationDate,
                        parentCreationDate,
                        createNote,
                        updateNote,
                        deleteNote,
                        true,
                    )
                }
            }
        }
    }
}