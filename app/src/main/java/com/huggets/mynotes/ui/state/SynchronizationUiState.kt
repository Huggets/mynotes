package com.huggets.mynotes.ui.state

/**
 * Represents the state of the synchronization activity.
 *
 * @property bluetoothSupported Whether Bluetooth is supported on the device.
 * @property bluetoothPermissionGranted Whether the app has the permission to use Bluetooth.
 * @property bluetoothEnabled Whether Bluetooth is enabled on the device.
 * @property bondedDevices A map of bonded devices, with the address as key and the name as value.
 * @property connecting Whether the app is connecting to a device.
 * @property connected Whether the app is connected to a device.
 * @property synchronisationError Whether there was an error during the synchronisation process.
 * @property synchronisationErrorMessage The error message of the synchronisation process. It is
 * meaningful only if [synchronisationError] is true.
 */
data class SynchronizationUiState(
    val bluetoothSupported: Boolean = false,
    val bluetoothPermissionGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val bondedDevices: Map<String, String> = emptyMap(),
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val synchronisationError: Boolean = false,
    val synchronisationErrorMessage: String = "",
)
