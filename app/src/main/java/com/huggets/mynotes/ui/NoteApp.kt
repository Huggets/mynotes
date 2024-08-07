package com.huggets.mynotes.ui

import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.NoteViewModel
import com.huggets.mynotes.theme.AppTheme
import com.huggets.mynotes.ui.Values.Animation
import com.huggets.mynotes.ui.Values.Animation.slideOffset
import com.huggets.mynotes.ui.activity.NoteEditor
import com.huggets.mynotes.ui.activity.NotesList
import com.huggets.mynotes.ui.activity.Synchronizer
import com.huggets.mynotes.ui.state.NoteItemUiState

/**
 * The main entry point of the app.
 *
 * @param noteViewModel The [NoteViewModel] which provides the data and the logic of the app.
 * @param quitApplication The callback to quit the app.
 * @param export The callback to export the notes to a file.
 * @param import The callback to import the notes from a file.
 */
@Composable
fun NoteApp(
    noteViewModel: NoteViewModel,
    quitApplication: () -> Unit,
    export: () -> Unit,
    import: () -> Unit,
) {
    val navigationController = rememberNavController()
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
            NavHost(
                navController = navigationController,
                startDestination = Destinations.generateViewNoteList(),
            ) {
                composable(
                    Destinations.viewNoteListRoute,
                    popEnterTransition = enterViewListTransition,
                    exitTransition = exitViewListTransition,
                ) {
                    NotesList(
                        appState = appState,
                        fabPosition = fabPosition,
                        quitApplication = quitApplication,
                        navigateUp = navigateUpWithReturnValue,
                        navigateToNote = navigateToNote,
                        createNote = createNote,
                        deleteNotes = deleteNotes,
                        export = export,
                        import = import,
                        startSynchronization = startSyncDataWithAnotherDevice,
                    )
                }

                composable(
                    Destinations.editNoteRoute,
                    enterTransition = enterEditNoteTransition,
                    exitTransition = exitEditNoteTransition,
                    popExitTransition = exitEditNotePopTransition,
                    popEnterTransition = enterEditNotePopTransition,
                ) { backStackEntry ->
                    NoteEditor(
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
                    NoteEditor(
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
                    Synchronizer(
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

/**
 * A [Saver] which saves and restores the [FabPosition].
 */
private val fabPositionSaver = object : Saver<FabPosition, Boolean> {
    override fun restore(value: Boolean): FabPosition {
        return if (value) FabPosition.Center else FabPosition.End
    }

    override fun SaverScope.save(value: FabPosition): Boolean {
        return value == FabPosition.Center
    }
}

// Lists of transitions and transition specs used when navigating between screens.

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
    slideIn(enterScreenIntOffsetSpec) { IntOffset(0, it.height) }

private val enterNewNoteRightTransition =
    slideIn(enterScreenIntOffsetSpec) { IntOffset(it.width, it.height) }

private val exitNewNoteCenterTransition =
    slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(0, it.height) }

private val exitNewNoteRightTransition =
    slideOut(exitScreenPermanentlyIntOffsetSpec) { IntOffset(it.width, it.height) }

private val enterViewListTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
    {
        if (isNewNote(initialState)) {
            null
        } else {
            enterScreenFromLeftTransition
        }
    }

private val exitViewListTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? =
    {
        if (isNewNote(targetState)) {
            null
        } else {
            exitScreenToLeftTransition
        }
    }

/**
 * Generates an [EnterTransition] for the [NoteEditor] based on the [FabPosition].
 */
private fun makeEnterEditNoteTransition(fabPosition: State<FabPosition>): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? {
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

private val exitEditNoteTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? =
    { exitScreenToLeftTransition }

private val enterEditNotePopTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
    { enterScreenFromLeftTransition }

/**
 * Generates an [ExitTransition] for the [NoteEditor] based on the [FabPosition].
 */
private fun makeExitEditNoteTransition(fabPosition: State<FabPosition>): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? {
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

private val enterDataSyncingTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? =
    { enterScreenFromRightTransition }

private val exitDataSyncingTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? =
    { exitScreenToRightTransition }

/**
 * Returns the creation date of the note from the [NavBackStackEntry].
 */
private fun getCreationDate(backStackEntryArgument: Bundle) = Date.fromString(
    backStackEntryArgument.getString(Destinations.ParametersName.noteCreationDate)!!
)

/**
 * Indicates whether the note is new based on the [NavBackStackEntry].
 */
private fun isNewNote(navBackStackEntry: NavBackStackEntry): Boolean {
    return navBackStackEntry.destination.route == Destinations.newNoteRoute
}
