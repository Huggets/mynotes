package com.huggets.mynotes.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.huggets.mynotes.R

/**
 * When displayDialog is true, show a Dialog that ask for a confirmation.
 *
 * @param displayDialog A mutable state that indicates whether the dialog should be displayed.
 * @param message The message to display in the dialog.
 * @param onConfirm The action to execute when the user confirms.
 * @param onDismiss The action to execute when the user dismisses.
 */
@Composable
fun ConfirmationDialog(
    displayDialog: MutableState<Boolean>,
    message: String,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val dismiss: () -> Unit = {
        displayDialog.value = false
        onDismiss()
    }
    val confirm: () -> Unit = {
        displayDialog.value = false
        onConfirm()
    }

    if (displayDialog.value) {
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = { Button(onClick = confirm) { Text(stringResource(R.string.yes)) } },
            dismissButton = { Button(onClick = dismiss) { Text(stringResource(R.string.no)) } },
            text = { Text(message) },
        )
    }
}

/**
 * When displayDialog is true, show a Dialog that ask for a confirmation.
 *
 * @param displayDialog A mutable state that indicates whether the dialog should be displayed.
 * @param messageProvider A lambda that returns the message to display in the dialog.
 * @param onConfirm The action to execute when the user confirms.
 * @param onDismiss The action to execute when the user dismisses.
 */
@Composable
fun ConfirmationDialog(
    displayDialog: MutableState<Boolean>,
    messageProvider: () -> String,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    ConfirmationDialog(
        displayDialog = displayDialog,
        message = messageProvider(),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Changes the behavior of the back button.
 *
 * @param onBackPress The callback to run when back is pressed.
 */
@Composable
fun BackPressHandler(onBackPress: () -> Unit = {}) {
    val onBackPressed = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPress()
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Add a callback called when back is pressed
    // Remove it when leaving the composition
    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher?.onBackPressedDispatcher?.addCallback(
            lifecycleOwner,
            onBackPressed
        )

        onDispose {
            onBackPressed.remove()
        }
    }
}

/**
 * An animated floating action button.
 *
 * Depending on the maximum width of the parent, the button can be either a regular FAB or an
 * extended FAB. The extended FAB is used when the parent is wider or equal to
 * [Values.minWidthRequiredExtendedFab].
 *
 * @param text The text to display in the FAB.
 * @param icon The icon to display in the FAB.
 * @param parentMaxWidth The maximum width of the parent.
 * @param isVisible A lambda that returns whether the FAB should be visible.
 * @param onClick The action to execute when the FAB is clicked.
 */
@Composable
fun AnimatedFab(
    text: String,
    icon: @Composable () -> Unit = {},
    parentMaxWidth: Dp = Values.minWidthRequiredExtendedFab,
    isVisible: () -> Boolean = { true },
    onClick: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = isVisible(),
        enter = scaleIn(Values.emphasizedFloat),
        exit = scaleOut(Values.emphasizedFloat),
    ) {
        if (parentMaxWidth <= Values.minWidthRequiredExtendedFab) {
            FloatingActionButton(
                onClick = onClick,
                content = icon,
            )
        } else {
            ExtendedFloatingActionButton(
                onClick = onClick,
                icon = icon,
                text = { Text(text) },
            )
        }
    }
}

/**
 * An [IconButton] that appears and disappears with an animation.
 *
 * @param visibleStateProvider A lambda that returns whether the button should be visible.
 * @param onClick The action to execute when the button is clicked.
 * @param icon The icon to display in the button.
 */
@Composable
fun AnimatedIconButton(
    visibleStateProvider: () -> Boolean = { true },
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visibleStateProvider(),
        enter = fadeIn(Values.emphasizedFloat),
        exit = fadeOut(Values.emphasizedFloat),
    ) {
        IconButton(onClick = onClick, content = icon)
    }
}