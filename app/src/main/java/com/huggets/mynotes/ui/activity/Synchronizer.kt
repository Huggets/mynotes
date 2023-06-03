package com.huggets.mynotes.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huggets.mynotes.R
import com.huggets.mynotes.ui.BackPressHandler
import com.huggets.mynotes.ui.ConfirmationDialog
import com.huggets.mynotes.ui.Values
import com.huggets.mynotes.ui.state.NoteAppUiState

/**
 * The data synchronizer.
 *
 * It synchronizes the data of this device with the data of another device.
 *
 * @param appState The app state.
 * @param navigateUp The lambda to call to navigate up.
 * @param cancelSync The lambda to call to cancel the synchronization.
 * @param syncWithDevice The lambda to call to synchronize data with a device.
 */
@Composable
fun Synchronizer(
    appState: State<NoteAppUiState>,
    navigateUp: () -> Unit = {},
    cancelSync: () -> Unit = {},
    syncWithDevice: (deviceAddress: String) -> Unit = {},
) {
    val showCancelDialog = rememberSaveable { mutableStateOf(false) }
    var navigateUpWhenCancelling by rememberSaveable { mutableStateOf(false) }

    BackPressHandler {
        if (appState.value.synchronizationState.connecting || appState.value.synchronizationState.connected) {
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

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarWasNotShown by remember(appState.value.synchronizationState.synchronisationError) {
        mutableStateOf(appState.value.synchronizationState.synchronisationError)
    }
    val errorMessagePrefix = stringResource(R.string.synchronization_error)

    LaunchedEffect(appState.value.synchronizationState.synchronisationError) {
        if (appState.value.synchronizationState.synchronisationError && snackbarWasNotShown) {
            val message =
                "$errorMessagePrefix: ${appState.value.synchronizationState.synchronisationErrorMessage}"
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            snackbarWasNotShown = false
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                onCancel = {
                    if (appState.value.synchronizationState.connecting || appState.value.synchronizationState.connected) {
                        navigateUpWhenCancelling = true
                        showCancelDialog.value = true
                    } else {
                        navigateUp()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        MainContent(
            appState = appState,
            modifier = Modifier
                .padding(it)
                .then(Values.Modifier.paddingMaxSize),
            syncWithDevice = syncWithDevice,
        )
    }
}

/**
 * The main content of the synchronization activity.
 *
 * @param appState The app state.
 * @param modifier The modifier to apply to the content.
 * @param syncWithDevice The lambda to call to synchronize data with a device.
 */
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

        if (!appState.value.synchronizationState.bluetoothSupported) {
            Text(
                text = stringResource(R.string.bluetooth_not_supported),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else if (!appState.value.synchronizationState.bluetoothPermissionGranted) {
            Text(
                text = stringResource(R.string.error_bluetooth_permission_not_granted),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else if (!appState.value.synchronizationState.bluetoothEnabled) {
            Text(
                text = stringResource(R.string.error_bluetooth_disabled),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else if (appState.value.synchronizationState.connecting) {
            ProgressIndicator()
        } else if (appState.value.synchronizationState.connected) {
            Text(
                text = stringResource(R.string.synchronizing_notes),
                modifier = center,
                textAlign = TextAlign.Center,
            )
        } else {
            BluetoothDevices(appState, syncWithDevice)
        }
    }
}

/**
 * A circular progress indicator
 */
@Composable
private fun ProgressIndicator() {
    Box(Values.Modifier.maxSize) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

/**
 * The app bar of the synchronization activity.
 *
 * @param onCancel The lambda to call to cancel the synchronization.
 */
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

/**
 * The list of Bluetooth devices.
 *
 * @param appState The app state.
 * @param syncWithDevice The lambda to call to synchronize data with a device.
 */
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
        if (appState.value.synchronizationState.bondedDevices.isEmpty()) {
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
            appState.value.synchronizationState.bondedDevices.forEach { device ->
                item(device.key) {
                    Device(
                        name = device.value,
                        onClick = { syncWithDevice(device.key) },
                    )
                }
            }
        }
    }
}

/**
 * A single bluetooth device.
 *
 * @param name The name of the device.
 * @param onClick The action to perform when the device is clicked.
 */
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
