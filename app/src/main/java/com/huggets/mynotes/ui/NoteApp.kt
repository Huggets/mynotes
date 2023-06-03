package com.huggets.mynotes.ui

import android.os.Bundle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.FabPosition
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
import com.huggets.mynotes.ui.Values.Animation
import com.huggets.mynotes.ui.Values.Animation.slideOffset
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

@OptIn(ExperimentalAnimationApi::class)
private val enterNewNoteCenterTransition =
    (scaleIn(
        transformOrigin = TransformOrigin(0.5f, 1f),
        animationSpec = enterScreenFloatSpec,
    ) + slideIn(enterScreenIntOffsetSpec) { IntOffset(0, it.height) })

@OptIn(ExperimentalAnimationApi::class)
private val enterNewNoteRightTransition =
    (scaleIn(
        transformOrigin = TransformOrigin(1f, 1f),
        animationSpec = enterScreenFloatSpec,
    ) + slideIn(enterScreenIntOffsetSpec) { IntOffset(it.width, it.height) })

@OptIn(ExperimentalAnimationApi::class)
private val exitNewNoteCenterTransition =
    (scaleOut(
        transformOrigin = TransformOrigin(0.5f, 1f),
        animationSpec = exitScreenPermanentlyFloatSpec,
    ) + slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(0, it.height) })

@OptIn(ExperimentalAnimationApi::class)
private val exitNewNoteRightTransition =
    (scaleOut(
        transformOrigin = TransformOrigin(1f, 1f),
        animationSpec = exitScreenPermanentlyFloatSpec,
    ) + slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(it.width, it.height) })

@OptIn(ExperimentalAnimationApi::class)
private val enterViewListTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
    {
        if (isNewNote(initialState)) {
            null
        } else {
            enterScreenFromLeftTransition
        }
    }

@OptIn(ExperimentalAnimationApi::class)
private val exitViewListTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
    {
        if (isNewNote(targetState)) {
            null
        } else {
            exitScreenToLeftTransition
        }
    }

@OptIn(ExperimentalAnimationApi::class)
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

@OptIn(ExperimentalAnimationApi::class)
private val exitEditNoteTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
    { exitScreenToLeftTransition }

@OptIn(ExperimentalAnimationApi::class)
private val enterEditNotePopTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
    { enterScreenFromLeftTransition }

@OptIn(ExperimentalAnimationApi::class)
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

@OptIn(ExperimentalAnimationApi::class)
private val enterDataSyncingTransition: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition? =
    { enterScreenFromRightTransition }

@OptIn(ExperimentalAnimationApi::class)
private val exitDataSyncingTransition: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition? =
    { exitScreenToRightTransition }

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
    val updateNote: (NoteItemUiState, () -> Unit) -> Unit = { note, onNoteUpdate ->
        noteViewModel.updateNote(note, onNoteUpdate)
    }
    val deleteNote: (creationDate: Date) -> Unit = { creationDate ->
        noteViewModel.deleteNote(creationDate)
    }
    val deleteNotes: (noteCreationDates: List<Date>) -> Unit = { noteCreationDates ->
        for (creationDate in noteCreationDates) {
            noteViewModel.deleteNote(creationDate)
        }
    }
    val startSyncDataWithAnotherDevice: () -> Unit = {
        noteViewModel.enableBluetooth()
        navigationController.navigate(Destinations.generateDataSyncing())
    }
    val syncWithBluetoothDevice: (deviceAddress: String) -> Unit =
        { deviceAddress ->
            noteViewModel.connectToBluetoothDevice(deviceAddress)
        }
    val cancelSync: () -> Unit = {
        noteViewModel.cancelBluetoothConnection()
    }
    val navigateUp: () -> Unit = {
        navigationController.popBackStack()
    }
    val navigateUpWithReturnValue = {
        navigationController.popBackStack()
    }
    val navigateToNote = { creationDate: Date, isNew: Boolean ->
        navigationController.navigate(Destinations.generateEditNote(creationDate, isNew))
    }

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
                        appState = appState,
                        fabPosition = fabPosition,
                        quitApplication = quitApplication,
                        navigateUp = navigateUpWithReturnValue,
                        navigateToNote = navigateToNote,
                        createNote = createNote,
                        deleteNotes = deleteNotes,
                        exportToXml = exportToXml,
                        importFromXml = importFromXml,
                        startSyncDataWithAnotherDevice = startSyncDataWithAnotherDevice,
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
                        appState = appState,
                        noteCreationDate = getCreationDate(backStackEntry.arguments!!),
                        isNew = false,
                        navigateUp = navigateUp,
                        navigateToNote = navigateToNote,
                        createNote = createNote,
                        saveNote = updateNote,
                        deleteNote = deleteNote,
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
                        appState = appState,
                        noteCreationDate = getCreationDate(backStackEntry.arguments!!),
                        isNew = true,
                        navigateUp = navigateUp,
                        navigateToNote = navigateToNote,
                        createNote = createNote,
                        saveNote = updateNote,
                        deleteNote = deleteNote,
                    )
                }
                composable(
                    Destinations.dataSyncingRoute,
                    enterTransition = enterDataSyncingTransition,
                    exitTransition = exitDataSyncingTransition,
                    popExitTransition = exitDataSyncingTransition,
                ) {
                    DataSyncingActivity(
                        appState = appState,
                        navigateUp = navigateUp,
                        cancelSync = cancelSync,
                        syncWithDevice = syncWithBluetoothDevice,
                    )
                }
            }
        }
    }
}