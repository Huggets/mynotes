@file:OptIn(ExperimentalAnimationApi::class)

package com.huggets.mynotes.ui

import android.os.Bundle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.FabPosition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

private val fabPositionSaver = object : Saver<FabPosition, Boolean> {
    override fun restore(value: Boolean): FabPosition {
        return if (value) FabPosition.Center else FabPosition.End
    }

    override fun SaverScope.save(value: FabPosition): Boolean {
        return value == FabPosition.Center
    }
}

private val enterScreenIntOffsetSpec = Animation.emphasizedDecelerate<IntOffset>()
private val enterScreenFloatSpec = tween<Float>(enterScreenIntOffsetSpec.durationMillis)
private val exitScreenPermanentlyIntOffsetSpec = Animation.emphasizedAccelerate<IntOffset>()
private val exitScreenPermanentlyFloatSpec =
    tween<Float>(exitScreenPermanentlyIntOffsetSpec.durationMillis)

private val enterScreenFromRightTransition =
    (fadeIn(enterScreenFloatSpec) +
            slideInHorizontally(enterScreenIntOffsetSpec) { (it * slideOffset).toInt() })
private val enterScreenFromLeftTransition =
    (fadeIn(enterScreenFloatSpec) +
            slideInHorizontally(enterScreenIntOffsetSpec) { -(it * slideOffset).toInt() })
private val exitScreenToRightTransition =
    (fadeOut(exitScreenPermanentlyFloatSpec) +
            slideOutHorizontally(exitScreenPermanentlyIntOffsetSpec) {
                (it * slideOffset).toInt()
            })
private val exitScreenToLeftTransition =
    (fadeOut(exitScreenPermanentlyFloatSpec) +
            slideOutHorizontally(exitScreenPermanentlyIntOffsetSpec) {
                -(it * slideOffset).toInt()
            })
private val enterNewNoteCenterTransition =
    (scaleIn(
        transformOrigin = TransformOrigin(0.5f, 1f),
        animationSpec = enterScreenFloatSpec,
    ) + slideIn(enterScreenIntOffsetSpec) { IntOffset(0, it.height) })
private val enterNewNoteRightTransition =
    (scaleIn(
        transformOrigin = TransformOrigin(1f, 1f),
        animationSpec = enterScreenFloatSpec,
    ) + slideIn(enterScreenIntOffsetSpec) { IntOffset(it.width, it.height) })
private val exitNewNoteCenterTransition =
    (scaleOut(
        transformOrigin = TransformOrigin(0.5f, 1f),
        animationSpec = exitScreenPermanentlyFloatSpec,
    ) + slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(0, it.height) })
private val exitNewNoteRightTransition =
    (scaleOut(
        transformOrigin = TransformOrigin(1f, 1f),
        animationSpec = exitScreenPermanentlyFloatSpec,
    ) + slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(it.width, it.height) })

private val enterViewListTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
    {
        if (isNewNote(initialState)) {
            null
        } else {
            enterScreenFromLeftTransition
        }
    }
private val exitViewListTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
    {
        if (isNewNote(targetState)) {
            null
        } else {
            exitScreenToLeftTransition
        }
    }

private fun makeEnterEditNoteTransition(fabPosition: State<FabPosition>): AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? {
    return {
        if (
            this.initialState.destination.route == Destinations.viewNoteListRoute &&
            isNewNote(targetState)
        ) {
            if (fabPosition.value == FabPosition.Center) {
                enterNewNoteCenterTransition
            } else {
                enterNewNoteRightTransition
            }
        } else {
            enterScreenFromRightTransition
        }
    }
}

private val exitEditNoteTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
    { exitScreenToLeftTransition }
private val enterEditNotePopTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
    { enterScreenFromLeftTransition }

private fun makeExitEditNoteTransition(fabPosition: State<FabPosition>): AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? {
    return {
        if (
            targetState.destination.route == Destinations.viewNoteListRoute &&
            isNewNote(initialState)
        ) {
            if (fabPosition.value == FabPosition.Center) {
                exitNewNoteCenterTransition
            } else {
                exitNewNoteRightTransition
            }
        } else {
            exitScreenToRightTransition
        }
    }
}

private fun getCreationDate(backStackEntryArgument: Bundle) = Date.fromString(
    backStackEntryArgument.getString(Destinations.ParametersName.noteCreationDate)!!
)

private fun isNewNote(navBackStackEntry: NavBackStackEntry): Boolean {
    return navBackStackEntry.destination.route == Destinations.newNoteRoute
}

@OptIn(ExperimentalAnimationApi::class)
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
    val fabPosition = rememberSaveable(stateSaver = fabPositionSaver) {
        mutableStateOf(FabPosition.Center)
    }

    val createNote: (parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) -> Unit =
        { parentCreationDate, onCreationDone ->
            noteViewModel.createNote(parentCreationDate, onCreationDone)
        }
    val updateNote: (NoteItemUiState) -> Unit = { note ->
        noteViewModel.updateNote(note)
    }
    val deleteNote: (creationDate: Date) -> Unit = { creationDate ->
        noteViewModel.deleteNote(creationDate)
    }
    val deleteNotes: (noteCreationDates: List<Date>) -> Unit = { noteCreationDates ->
        for (creationDate in noteCreationDates) {
            noteViewModel.deleteNote(creationDate)
        }
    }

    noteViewModel.syncUiState()

    val enterEditNoteTransition = remember { makeEnterEditNoteTransition(fabPosition) }
    val exitEditNotePopTransition = remember { makeExitEditNoteTransition(fabPosition) }

    AppTheme {
        Surface {
            AnimatedNavHost(
                navController = navigationController,
                startDestination = Destinations.generateViewNoteList(),
            ) {
                composable(
                    Destinations.viewNoteListRoute,
                    popEnterTransition = enterViewListTransition,
                    exitTransition = exitViewListTransition,
                ) {
                    NoteListActivity(
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
                    enterTransition = enterEditNoteTransition,
                    exitTransition = exitEditNoteTransition,
                    popExitTransition = exitEditNotePopTransition,
                    popEnterTransition = enterEditNotePopTransition,
                ) { backStackEntry ->
                    NoteEditingActivity(
                        navigationController,
                        appState,
                        getCreationDate(backStackEntry.arguments!!),
                        createNote,
                        updateNote,
                        deleteNote,
                        false,
                    )
                }
                composable(
                    Destinations.newNoteRoute,
                    enterTransition = enterEditNoteTransition,
                    exitTransition = exitEditNoteTransition,
                    popExitTransition = exitEditNotePopTransition,
                    popEnterTransition = enterEditNotePopTransition,
                ) { backStackEntry ->
                    NoteEditingActivity(
                        navigationController,
                        appState,
                        getCreationDate(backStackEntry.arguments!!),
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