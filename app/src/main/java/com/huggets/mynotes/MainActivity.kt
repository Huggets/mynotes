package com.huggets.mynotes

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.NoteViewModel
import com.huggets.mynotes.ui.NoteApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

class MainActivity : ComponentActivity() {

    /**
     * Activity launcher for requesting Bluetooth permission.
     */
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                bluetoothConnectionManager!!.onBluetoothPermissionGranted()
            } else {
                bluetoothConnectionManager!!.onBluetoothPermissionDenied()
            }
        }

    /**
     * Activity launcher for requesting Bluetooth activation.
     */
    private val requestBluetoothActivationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if (it.resultCode == RESULT_OK) {
                bluetoothConnectionManager!!.onBluetoothActivationRequestAccepted()
            } else {
                bluetoothConnectionManager!!.onBluetoothActivationRequestDenied()
            }
        }

    /**
     * Indicates whether the BLUETOOTH_CONNECT permission is granted.
     *
     * On Android 11 and below, this permission does not exist and is always granted.
     *
     * @return true if the permission is granted, false otherwise.
     */
    private fun isBluetoothPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Indicates whether this device supports Bluetooth.
     *
     * @return true if Bluetooth is supported, false otherwise.
     */
    private fun isBluetoothSupported(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    /**
     * Ask the user to activate Bluetooth.
     */
    private fun requestBluetoothActivation() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestBluetoothActivationLauncher.launch(enableBluetoothIntent)
    }

    /**
     * Ask the user to grant the BLUETOOTH_CONNECT permission.
     */
    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED -> {
                    bluetoothConnectionManager!!.onBluetoothPermissionGranted()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                    bluetoothConnectionManager!!.onBluetoothPermissionDenied()
                }

                else -> {
                    requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        } else {
            // On Android 11 and below, the BLUETOOTH_CONNECT permission does not exist and is
            // always granted.
            bluetoothConnectionManager!!.onBluetoothPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (bluetoothConnectionManager == null) {
            bluetoothConnectionManager = BluetoothConnectionManager(
                getSystemService(BluetoothManager::class.java),
                ::isBluetoothSupported,
                ::isBluetoothPermissionGranted,
            )

            bluetoothConnectionManager!!.setRequestBluetoothActivationCallback(::requestBluetoothActivation)
            bluetoothConnectionManager!!.setRequestBluetoothPermissionCallback(::requestBluetoothPermission)
        }

        val noteViewModel by viewModels<NoteViewModel>({
            MutableCreationExtras().also { extras ->
                extras[NoteViewModel.APPLICATION_KEY_EXTRAS] = application
                extras[NoteViewModel.BLUETOOTH_CONNECTION_MANAGER_KEY_EXTRAS] =
                    bluetoothConnectionManager!!
                extras[NoteViewModel.RESOURCES_KEY_EXTRAS] = resources
            }
        }) { NoteViewModel.Factory }

        val openDocumentLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                val fileChosen: Boolean
                val stream = if (it == null) {
                    fileChosen = false
                    null
                } else {
                    fileChosen = true
                    try {
                        runBlocking(Dispatchers.IO) {
                            applicationContext.contentResolver.openInputStream(it)
                        }
                    } catch (e: FileNotFoundException) {
                        null
                    }
                }
                noteViewModel.onImportedFileOpened(stream, fileChosen)
            }

        val createDocumentLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
                val fileChosen: Boolean
                val stream = if (it == null) {
                    fileChosen = false
                    null
                } else {
                    fileChosen = true
                    try {
                        runBlocking(Dispatchers.IO) {
                            applicationContext.contentResolver.openOutputStream(it, "wt")
                        }
                    } catch (e: FileNotFoundException) {
                        null
                    }
                }
                noteViewModel.onExportedFileOpened(stream, fileChosen)
            }

        val quitApplication: () -> Unit = { finish() }

        val importFromXml: () -> Unit = {
            noteViewModel.import {
                openDocumentLauncher.launch(arrayOf("text/plain"))
            }
        }
        val exportToXml: () -> Unit = {
            noteViewModel.export {
                Date.getCurrentTime().apply {
                    createDocumentLauncher.launch(
                        "notes_$year-$month-$day-$hour-$minute-$second.txt"
                    )
                }
            }
        }

        noteViewModel.syncUiState()

        setContent {
            NoteApp(
                quitApplication = quitApplication,
                exportToXml = exportToXml,
                importFromXml = importFromXml,
                noteViewModel = noteViewModel,
            )
        }
    }

    companion object {
        private var bluetoothConnectionManager: BluetoothConnectionManager? = null
    }
}