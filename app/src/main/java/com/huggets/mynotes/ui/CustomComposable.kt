package com.huggets.mynotes.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier

/**
 * When displayDialog is true, show a Dialog that ask for a confirmation.
 *
 * If the user confirm, run onConfirm and close the dialog.
 */
@Composable
fun ConfirmationDialog(
    displayDialog: MutableState<Boolean>,
    onConfirmation: () -> Unit,
    confirmationMessage: String,
    modifier: Modifier = Modifier,
) {
    val dismiss: () -> Unit = { displayDialog.value = false }
    val confirm: () -> Unit = {
        displayDialog.value = false
        onConfirmation()
    }

    if (displayDialog.value) {
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = { Button(onClick = confirm) { Text("Yes") } },
            dismissButton = { Button(onClick = dismiss) { Text("Cancel") } },
            text = { Text(confirmationMessage) },
            modifier = modifier
        )
    }
}