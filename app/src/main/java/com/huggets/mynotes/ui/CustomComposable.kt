package com.huggets.mynotes.ui

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.huggets.mynotes.R

/**
 * When displayDialog is true, show a Dialog that ask for a confirmation.
 *
 * If the user confirm, run onConfirm and close the dialog.
 */
@Composable
fun ConfirmationDialog(
    displayDialog: MutableState<Boolean>,
    onConfirmation: () -> Unit,
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val dismiss: () -> Unit = {
        displayDialog.value = false
        onDismiss()
    }
    val confirm: () -> Unit = {
        displayDialog.value = false
        onConfirmation()
    }

    if (displayDialog.value) {
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = { Button(onClick = confirm) { Text(stringResource(R.string.yes)) } },
            dismissButton = { Button(onClick = dismiss) { Text(stringResource(R.string.no)) } },
            text = { Text(message) },
            modifier = modifier
        )
    }
}

/**
 * Changes the behavior of the back button.
 *
 * @param onBackPress The callback to run when back is pressed.
 */
@Composable
fun BackPressHandler(
    onBackPress: () -> Unit,
) {
    val onBackPressed = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBackPress()
        }
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