package com.huggets.mynotes.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

/**
 * Manages the Bluetooth connection.
 *
 * @param bluetoothManager The Bluetooth manager used to do the bluetooth operations.
 * @property isBluetoothSupported A function that returns whether the Bluetooth is supported by the
 * device.
 * @property isBluetoothPermissionGranted A function that returns whether the Bluetooth permission is
 * granted.
 */
class BluetoothConnectionManager(
    bluetoothManager: BluetoothManager,
    val isBluetoothSupported: () -> Boolean,
    val isBluetoothPermissionGranted: () -> Boolean,
) {
    /**
     * The Bluetooth adapter used to do the bluetooth operations.
     */
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    /**
     * Whether the Bluetooth is enabled.
     */
    val bluetoothEnabled
        get() = bluetoothAdapter?.isEnabled == true

    /**
     * Function to call to enable the Bluetooth.
     */
    private var requestBluetoothActivationCallback: (() -> Unit)? = null

    /**
     * Function to call when the Bluetooth activation request is accepted.
     */
    private var bluetoothActivationRequestAcceptedCallback: (() -> Unit)? = null

    /**
     * Function to call when the Bluetooth activation request is denied.
     */
    private var bluetoothActivationRequestDeniedCallback: (() -> Unit)? = null

    /**
     * Function to call to request the Bluetooth permission.
     */
    private var requestBluetoothPermissionCallback: (() -> Unit)? = null

    /**
     * Function to call when the Bluetooth permission request is accepted.
     */
    private var bluetoothPermissionGrantedCallback: (() -> Unit)? = null

    /**
     * Function to call when the Bluetooth permission request is denied.
     */
    private var bluetoothPermissionDeniedCallback: (() -> Unit)? = null

    /**
     * A bluetooth server socket used to accept and establish a connection.
     */
    private var server: BluetoothServerSocket? = null

    /**
     * A bluetooth socket used to communicate with the other device.
     */
    private var socket: BluetoothSocket? = null

    /**
     * Temporary socket that may be created by the server during the connection.
     *
     * At the end of the connection process, only one socket between [serverSocket] and
     * [clientSocket] will be chosen and assigned to [socket]. The other one will be closed, and
     * both will be set to null.
     */
    private var serverSocket: BluetoothSocket? = null

    /**
     * Temporary socket that may be created by the client during the connection.
     *
     * At the end of the connection process, only one socket between [serverSocket] and
     * [clientSocket] will be chosen and assigned to [socket]. The other one will be closed, and
     * both will be set to null.
     */
    private var clientSocket: BluetoothSocket? = null

    /**
     * Set to true when trying to connect to a device.
     */
    private var isConnecting = false

    /**
     * @return A set of bluetooth devices that are paired with this device.
     */
    fun getBondedDevices(): Set<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            emptySet()
        }
    }

    /**
     * Requests the Bluetooth to be enabled.
     */
    fun requestBluetoothActivation() {
        requestBluetoothActivationCallback?.invoke()
    }

    /**
     * Sets the callback to call when the Bluetooth is requested to be enabled.
     */
    fun setRequestBluetoothActivationCallback(callback: () -> Unit) {
        requestBluetoothActivationCallback = callback
    }

    /**
     * Sets the callback to call when the Bluetooth activation request is accepted.
     */
    fun setOnBluetoothActivationRequestAcceptedCallback(callback: () -> Unit) {
        bluetoothActivationRequestAcceptedCallback = callback
    }

    /**
     * Called when the Bluetooth activation request is accepted.
     */
    fun onBluetoothActivationRequestAccepted() {
        bluetoothActivationRequestAcceptedCallback?.invoke()
    }

    /**
     * Sets the callback to call when the Bluetooth activation request is denied.
     */
    fun setOnBluetoothActivationRequestDeniedCallback(callback: () -> Unit) {
        bluetoothActivationRequestDeniedCallback = callback
    }

    /**
     * Called when the Bluetooth activation request is denied.
     */
    fun onBluetoothActivationRequestDenied() {
        bluetoothActivationRequestDeniedCallback?.invoke()
    }

    /**
     * Requests the Bluetooth permission.
     */
    fun requestBluetoothPermission() {
        requestBluetoothPermissionCallback?.invoke()
    }

    /**
     * Sets the callback to call when the Bluetooth permission is requested.
     */
    fun setRequestBluetoothPermissionCallback(callback: () -> Unit) {
        requestBluetoothPermissionCallback = callback
    }

    /**
     * Called when the Bluetooth permission is granted.
     */
    fun onBluetoothPermissionGranted() {
        bluetoothPermissionGrantedCallback?.invoke()
    }

    /**
     * Sets the callback to call when the Bluetooth permission is granted.
     */
    fun setOnBluetoothPermissionGrantedCallback(callback: () -> Unit) {
        bluetoothPermissionGrantedCallback = callback
    }

    /**
     * Called when the Bluetooth permission is denied.
     */
    fun onBluetoothPermissionDenied() {
        bluetoothPermissionDeniedCallback?.invoke()
    }

    /**
     * Sets the callback to call when the Bluetooth permission is denied.
     */
    fun setOnBluetoothPermissionDeniedCallback(callback: () -> Unit) {
        bluetoothPermissionDeniedCallback = callback
    }

    /**
     * Try connecting to the given device.
     *
     * @param device The device to connect to.
     * @param onConnectionEstablished The function to call when the connection is established.
     * @param onConnectionError The function to call when an error occurs during the connection.
     */
    fun connect(
        device: BluetoothDevice,
        onConnectionEstablished: () -> Unit = {},
        onConnectionError: (Exception) -> Unit = {},
    ) {
        // If the bluetooth adapter is null, bluetooth is not available.
        // If the device is already connecting, do nothing.

        if (bluetoothAdapter == null || isConnecting) {
            return
        }

        isConnecting = true

        var exception: Exception? = null

        // Try to connect to the device. Try to create a server socket and a client socket.
        // If both are created, choose one randomly and close the other one. If only one is created,
        // use it. If none are created, an exception is generated and is handled at the end of the
        // coroutine.

        CoroutineScope(Dispatchers.IO).launch {
            server = try {
                bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyNotes", SERVICE_UUID)
            } catch (e: IOException) {
                exception = e
                return@launch
            } catch (e: SecurityException) {
                exception = e
                return@launch
            }

            clientSocket = try {
                device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            } catch (e: IOException) {
                exception = e
                return@launch
            } catch (e: SecurityException) {
                exception = e
                return@launch
            }

            // Try to create a socket through a server. Return true if an error occurs.

            val serverJob = async {
                try {
                    serverSocket = server!!.accept(30000)
                    false
                } catch (e: IOException) {
                    // Keep the first exception thrown.
                    if (exception == null) {
                        exception = e
                    }
                    true
                }
            }

            // Try connect to a server. Return true if an error occurs.

            val clientJob = async {
                try {
                    clientSocket!!.connect()
                    false
                } catch (e: IOException) {
                    // Keep the first exception
                    if (exception == null) {
                        exception = e
                    }
                    true
                } catch (e: SecurityException) {
                    // Keep the first exception
                    if (exception == null) {
                        exception = e
                    }
                    true
                }
            }

            // Wait the client job to finish (the client usually stops first).
            val clientError = clientJob.await()

            // If the client did not connect, wait the server job to finish.
            // If the client connected, then if the server did not connect stop it, otherwise wait
            // its job to finish.
            val serverError = if (clientError) {
                serverJob.await()
            } else {
                if (!serverJob.isCompleted) {
                    server?.close()
                    server = null
                    serverJob.join()
                    true
                } else {
                    serverJob.await()
                }
            }

            if (!clientError && !serverError) {
                // Both sockets are connected, choose randomly which one to keep.

                val receivingBuffer = ByteArray(2)
                val sendingBuffer = ByteArray(2)
                var randomValue: Int

                do {
                    randomValue = Random.Default.nextInt(0, 2).apply {
                        sendingBuffer[0] = toByte()
                        sendingBuffer[1] = toByte()
                    }

                    try {
                        withContext(Dispatchers.IO) {
                            val ioException = IOException("Connection cancelled")
                            clientSocket?.outputStream?.write(sendingBuffer, 0, 1)
                                ?: throw ioException
                            serverSocket?.outputStream?.write(sendingBuffer, 1, 1)
                                ?: throw ioException
                            clientSocket?.inputStream?.read(receivingBuffer, 0, 1)
                                ?: throw ioException
                            serverSocket?.inputStream?.read(receivingBuffer, 1, 1)
                                ?: throw ioException
                        }
                    } catch (e: IOException) {
                        exception = e
                        return@launch
                    }
                } while (receivingBuffer[0] == sendingBuffer[0])

                if (randomValue == 0) {
                    socket = clientSocket
                    serverSocket?.close()
                } else {
                    socket = serverSocket
                    clientSocket?.close()
                }
            } else if (clientError xor serverError) {
                // Only one socket is connected, use it.

                if (clientError) {
                    socket = serverSocket
                    clientSocket?.close()
                } else {
                    socket = clientSocket
                    serverSocket?.close()
                }
                exception = null
            }
        }.invokeOnCompletion {
            // If an exception occurred (no devices connected), call the error callback.
            // Otherwise, call the connection established callback.

            val exceptionSaved = exception

            server?.close()
            server = null
            clientSocket = null
            serverSocket = null

            if (exceptionSaved != null) {
                onConnectionError(exceptionSaved)
                stopConnection()
            } else {
                onConnectionEstablished()
            }

            isConnecting = false
        }
    }

    /**
     * Stops the connection.
     */
    fun stopConnection() {
        server?.close()
        server = null

        serverSocket?.close()
        serverSocket = null
        clientSocket?.close()
        clientSocket = null

        socket?.close()
        socket = null

        isConnecting = false
    }

    /**
     * Reads data sent from the remote device.
     */
    fun readData(buffer: ByteArray, off: Int = 0, len: Int = buffer.size - off): Int {
        return socket?.inputStream?.read(buffer, off, len) ?: -1
    }

    /**
     * Sends data to the remote device.
     */
    fun sendData(buffer: ByteArray, off: Int = 0, len: Int = buffer.size - off) {
        socket?.outputStream?.write(buffer, off, len)
    }

    companion object {
        /**
         * The UUID of the service used to connect to the remote device.
         */
        private val SERVICE_UUID = UUID.fromString("02b5ddfd-623a-4a60-8de4-6c399a1cf259")
    }
}