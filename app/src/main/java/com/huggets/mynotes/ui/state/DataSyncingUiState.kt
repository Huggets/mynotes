package com.huggets.mynotes.ui.state

data class DataSyncingUiState(
    val bluetoothSupported: Boolean = false,
    val bluetoothPermissionGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    // Address and name
    val bondedDevices: Map<String, String> = emptyMap(),
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val synchronisationError: Boolean = false,
    val synchronisationErrorMessage: String = "",
)
