package com.huggets.mynotes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huggets.mynotes.ui.state.NoteAppUiState

@Composable
fun DataSyncingActivity(
    appState: State<NoteAppUiState>,
    syncWithDevice: (deviceAddress: String) -> Unit,
    cancelSync: () -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancelSyncAndNavigateUp = {
        cancelSync()
        navigateUp()
    }
    val onBackPress = {
        if (appState.value.dataSyncingUiState.connecting || appState.value.dataSyncingUiState.connected) {
            cancelSync()
        } else {
            cancelSyncAndNavigateUp()
        }
    }
    BackPressHandler(onBackPress)

    Scaffold(
        modifier = modifier,
        topBar = { AppBar(cancelSyncAndNavigateUp) },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(Value.smallPadding)
                .fillMaxSize()
        ) {
            if (appState.value.dataSyncingUiState.synchronisationError) {
                Text(
                    text = "error: ${appState.value.dataSyncingUiState.synchronisationErrorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            if (!appState.value.dataSyncingUiState.bluetoothAvailable) {
                Text(
                    text = "Bluetooth is not available on this device",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else if (!appState.value.dataSyncingUiState.bluetoothEnabled) {
                val message = "Bluetooth disabled. Please enable it if you want to sync your notes"
                Text(
                    text = message,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                )
            } else if (appState.value.dataSyncingUiState.connecting) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else if (appState.value.dataSyncingUiState.connected) {
                Text(
                    text = "Connected",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                Parameters(appState, syncWithDevice)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text("Data syncing") },
        navigationIcon = {
            IconButton(onClick = { navigateUp() }) {
                Icon(Icons.Filled.Close, contentDescription = "Stop syncing")
            }
        },
    )
}

@Composable
private fun Parameters(
    appState: State<NoteAppUiState>,
    syncWithDevice: (deviceAddress: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        appState.value.dataSyncingUiState.bondedDevices.forEach { device ->
            item(device.key) {
                Button(
                    onClick = { syncWithDevice(device.key) },
                    modifier = Modifier
                        .padding(Value.smallSpacing)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(24.dp),
                    ) {
                    Text(device.value)
                }
            }
        }
    }
}