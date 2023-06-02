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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huggets.mynotes.R
import com.huggets.mynotes.ui.state.NoteAppUiState

@Composable
fun DataSyncingActivity(
    appState: State<NoteAppUiState>,
    navigateUp: () -> Unit = {},
    cancelSync: () -> Unit = {},
    syncWithDevice: (deviceAddress: String) -> Unit = {},
) {
    val showCancelDialog = rememberSaveable { mutableStateOf(false) }
    var navigateUpWhenCancelling by rememberSaveable { mutableStateOf(false) }

    BackPressHandler {
        if (appState.value.dataSyncingUiState.connecting || appState.value.dataSyncingUiState.connected) {
            showCancelDialog.value = true
        } else {
            navigateUp()
        }
    }

    ConfirmationDialog(
        displayDialog = showCancelDialog,
        message = stringResource(R.string.confirmation_message_cancel_synchronization),
        onConfirm = {
            cancelSync()

            if (navigateUpWhenCancelling) {
                navigateUp()
            }
        },
        onDismiss = { navigateUpWhenCancelling = false },
    )

    Scaffold(
        topBar = {
            AppBar(
                onCancel = {
                    if (appState.value.dataSyncingUiState.connecting || appState.value.dataSyncingUiState.connected) {
                        navigateUpWhenCancelling = true
                        showCancelDialog.value = true
                    } else {
                        navigateUp()
                    }
                }
            )
        },
    ) {
        MainContent(
            appState = appState,
            modifier = Modifier.padding(it).then(Values.Modifier.paddingMaxSize),
            syncWithDevice = syncWithDevice,
        )
    }
}

@Composable
private fun MainContent(
    appState: State<NoteAppUiState>,
    modifier: Modifier = Modifier,
    syncWithDevice: (deviceAddress: String) -> Unit = {},
) {
    Box(modifier) {
        val center = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()

        if (!appState.value.dataSyncingUiState.bluetoothSupported) {
            Text(
                text = stringResource(R.string.bluetooth_not_supported),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else if (!appState.value.dataSyncingUiState.bluetoothPermissionGranted) {
            Text(
                text = stringResource(R.string.error_bluetooth_permission_not_granted),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else if (!appState.value.dataSyncingUiState.bluetoothEnabled) {
            Text(
                text = stringResource(R.string.error_bluetooth_disabled),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else if (appState.value.dataSyncingUiState.connecting) {
            ProgressIndicator()
        } else if (appState.value.dataSyncingUiState.connected) {
            Text(
                text = stringResource(R.string.synchronizing_notes),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else {
            Column {
                if (appState.value.dataSyncingUiState.synchronisationError) {
                    Text(
                        text = stringResource(R.string.synchronization_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(0.dp, Values.smallPadding)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                BluetoothDevices(appState, syncWithDevice)
            }
        }
    }
}

@Composable
private fun ProgressIndicator() {
    Box(Values.Modifier.maxSize) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    onCancel: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.data_syncing_activity_name)) },
        navigationIcon = {
            IconButton(onClick = { onCancel() }) {
                Icon(Icons.Filled.Close, stringResource(R.string.stop_synchronization))
            }
        },
    )
}

@Composable
private fun BluetoothDevices(
    appState: State<NoteAppUiState>,
    syncWithDevice: (deviceAddress: String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Values.smallSpacing),
    ) {
        item(0) {
            Text(
                text = stringResource(R.string.paired_devices_information),
                modifier = Values.Modifier.maxWidth.padding(0.dp, Values.smallPadding),
                textAlign = TextAlign.Center,
            )
        }
        if (appState.value.dataSyncingUiState.bondedDevices.isEmpty()) {
            item(1) {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.no_paired_devices),
                        modifier = Modifier
                            .padding(0.dp, Values.smallPadding)
                            .align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            appState.value.dataSyncingUiState.bondedDevices.forEach { device ->
                item(device.key) {
                    Device(
                        name = device.value,
                        onClick = { syncWithDevice(device.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Device(
    name: String,
    onClick: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        shape = ShapeDefaults.Small,
        modifier = Values.Modifier.maxWidth,
    ) {
        Text(
            text = name,
            fontSize = Values.normalFontSize,
            fontWeight = FontWeight.Bold,
            modifier = Values.Modifier.smallPadding,
        )
    }
}
