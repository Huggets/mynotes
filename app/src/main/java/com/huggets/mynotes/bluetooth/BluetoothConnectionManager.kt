package com.huggets.mynotes.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

class BluetoothConnectionManager(
    bluetoothManager: BluetoothManager,
    packageManager: PackageManager,
) {
    val bluetoothAvailable =
        packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val bluetoothEnabled
        get() = bluetoothAdapter?.isEnabled == true

    private var requestBluetoothActivationCallback: (() -> Unit)? = null
    private var bluetoothActivationRequestAcceptedCallback: (() -> Unit)? = null
    private var bluetoothActivationRequestDeniedCallback: (() -> Unit)? = null

    private var server: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null

    private var serverSocket: BluetoothSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private var isConnecting = false

    val bondedDevices: Set<BluetoothDevice>
        @SuppressLint("MissingPermission")
        get() = bluetoothAdapter?.bondedDevices ?: emptySet()

    fun requestBluetoothActivation() {
        requestBluetoothActivationCallback?.invoke()
    }

    fun setRequestBluetoothActivationCallback(callback: () -> Unit) {
        requestBluetoothActivationCallback = callback
    }

    fun onBluetoothActivationRequestAccepted() {
        bluetoothActivationRequestAcceptedCallback?.invoke()
    }

    fun setOnBluetoothActivationRequestAcceptedCallback(callback: () -> Unit) {
        bluetoothActivationRequestAcceptedCallback = callback
    }

    fun onBluetoothActivationRequestDenied() {
        bluetoothActivationRequestDeniedCallback?.invoke()
    }

    fun setOnBluetoothActivationRequestDeniedCallback(callback: () -> Unit) {
        bluetoothActivationRequestDeniedCallback = callback
    }

    @SuppressLint("MissingPermission")
    fun connect(
        device: BluetoothDevice,
        onConnectionEstablished: () -> Unit = {},
        onConnectionError: (IOException) -> Unit = {},
    ) {
        if (bluetoothAdapter == null || isConnecting) {
            return
        }


        isConnecting = true

        var exception: IOException? = null

        CoroutineScope(Dispatchers.IO).launch {
            server = try {
                bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyNotes", SERVICE_UUID)
            } catch (e: IOException) {
                exception = e
                return@launch
            }

            clientSocket = try {
                device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            } catch (e: IOException) {
                exception = e
                return@launch
            }

            val serverJob = async {
                try {
                    serverSocket = server!!.accept(30000)
                    false
                } catch (e: IOException) {
                    // Keep the first exception thrown
                    if (exception == null) {
                        exception = e
                    }
                    true
                }
            }

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
                }
            }

            val clientError = clientJob.await()
            val serverError = if (!clientError) {

                withContext(Dispatchers.IO) {
                    Thread.sleep(1000)
                }
                if (!serverJob.isCompleted) {
                    server?.close()
                    server = null
                    serverJob.join()
                    true
                } else {
                    serverJob.await()
                }
            } else {
                serverJob.await()
            }

            if (!clientError && !serverError) {
                // Both sockets are connected
                // Choose randomly which one to keep

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

    fun stopConnection() {
        server?.close()
        server = null

        serverSocket?.close()
        serverSocket = null
        clientSocket?.close()
        clientSocket = null

        socket?.close()
        socket = null
    }

    @Throws(IOException::class)
    fun readData(buffer: ByteArray, off: Int = 0, len: Int = buffer.size - off): Int {
        return socket?.inputStream?.read(buffer, off, len) ?: -1
    }

    @Throws(IOException::class)
    fun writeData(buffer: ByteArray, off: Int = 0, len: Int = buffer.size - off) {
        socket?.outputStream?.write(buffer, off, len)
    }

    companion object {
        private val SERVICE_UUID = UUID.fromString("02b5ddfd-623a-4a60-8de4-6c399a1cf259")
    }
}