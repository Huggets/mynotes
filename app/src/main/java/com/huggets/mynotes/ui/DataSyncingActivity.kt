package com.huggets.mynotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huggets.mynotes.ui.state.NoteAppUiState

@Composable
fun DataSyncingActivity(
    appState: State<NoteAppUiState>,
    syncWithDevice: (deviceAddress: String) -> Unit,
    cancelSync: () -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showCancelDialog = rememberSaveable { mutableStateOf(false) }
    var navigateUpWhenCancelling by rememberSaveable { mutableStateOf(false) }

    val onCancelDialogDismiss = {
        navigateUpWhenCancelling = false
    }
    val cancel = {
        cancelSync()

        if (navigateUpWhenCancelling) {
            navigateUp()
        }
    }
    val cancelAndNavigateUp = {
        if (appState.value.dataSyncingUiState.connecting || appState.value.dataSyncingUiState.connected) {
            navigateUpWhenCancelling = true
            showCancelDialog.value = true
        } else {
            navigateUp()
        }
    }

    val onBackPress = {
        if (appState.value.dataSyncingUiState.connecting || appState.value.dataSyncingUiState.connected) {
            showCancelDialog.value = true
        } else {
            navigateUp()
        }
    }
    BackPressHandler(onBackPress)

    ConfirmationDialog(
        displayDialog = showCancelDialog,
        onConfirmation = cancel,
        message = "Are you sure you want to cancel synchronization?",
        onDismiss = onCancelDialogDismiss,
    )

    Scaffold(
        modifier = modifier,
        topBar = { AppBar(cancelAndNavigateUp) },
    ) {
        Box(
            Modifier
                .padding(it)
                .padding(Value.smallPadding)
                .fillMaxSize()
        ) {
            val center = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()

            if (!appState.value.dataSyncingUiState.bluetoothSupported) {
                val message = "Bluetooth is not supported on this device."
                Text(
                    text = message,
                    modifier = center,
                    textAlign = TextAlign.Center,
                )
            } else if (!appState.value.dataSyncingUiState.bluetoothPermissionGranted) {
                val message =
                    "Bluetooth permission is not granted. Please grant it if you want to sync" +
                            " your notes.\n"
                Text(
                    text = message,
                    modifier = center,
                    textAlign = TextAlign.Center,
                )
            } else if (!appState.value.dataSyncingUiState.bluetoothEnabled) {
                val message =
                    "Bluetooth disabled. Please enable it if you want to sync your notes."
                Text(
                    text = message,
                    modifier = center,
                    textAlign = TextAlign.Center,
                )
            } else if (appState.value.dataSyncingUiState.connecting) {
                SyncingIndicator()
            } else if (appState.value.dataSyncingUiState.connected) {
                val message = "Connected! Syncing your notes..."
                Text(
                    text = message,
                    modifier = center,
                    textAlign = TextAlign.Center,
                )
            } else {
                Column {
                    if (appState.value.dataSyncingUiState.synchronisationError) {
                        Text(
                            text = "Synchronization error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(0.dp, Value.smallPadding)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    BluetoothDevices(appState, syncWithDevice)
                }
            }
        }
    }
}

@Composable
private fun SyncingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text("Data syncing") },
        navigationIcon = {
            IconButton(onClick = { onCancel() }) {
                Icon(Icons.Filled.Close, contentDescription = "Stop syncing")
            }
        },
    )
}

@Composable
private fun BluetoothDevices(
    appState: State<NoteAppUiState>,
    syncWithDevice: (deviceAddress: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Value.smallSpacing),
        modifier = modifier,
    ) {
        item(0) {
            Text(
                text = "If you don't see your device, please pair it with your phone first." +
                        " (You can do it in your phone's Bluetooth settings.)",
                modifier = Modifier
                    .padding(0.dp, Value.smallPadding)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        if (appState.value.dataSyncingUiState.bondedDevices.isEmpty()) {
            item(1) {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = "No paired devices.",
                        modifier = Modifier
                            .padding(0.dp, Value.smallPadding)
                            .align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            appState.value.dataSyncingUiState.bondedDevices.forEach { device ->
                val syncWithClickedDevice = { syncWithDevice(device.key) }

                item(device.key) {
                    Device(device.value, syncWithClickedDevice)
                }
            }
        }
    }
}

@Composable
private fun Device(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = ShapeDefaults.Small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(Value.smallPadding),
        )
    }
}
