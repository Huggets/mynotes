package com.huggets.mynotes

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.data.NoteViewModel
import com.huggets.mynotes.ui.NoteApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.Calendar

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

        var showSnackbar: MutableState<Boolean>? = null
        var snackbarMessage: MutableState<String>? = null

        val createDocument =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
                if (it != null) {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            try {
                                applicationContext.contentResolver.openOutputStream(it, "wt")
                                    ?.let { stream ->
                                        noteViewModel.exportToXml(stream)
                                    }
                            } catch (e: FileNotFoundException) {
                                // Show an error message in the log and in a snackbar
                                Log.e("MainActivity", e.stackTraceToString())

                                snackbarMessage?.value =
                                    resources.getString(R.string.error_export_xml_writing_file)
                                showSnackbar?.value = true
                            }
                        }
                    }
                }
            }
        val readDocument =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                if (it != null) {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            try {
                                applicationContext.contentResolver.openInputStream(it)
                                    ?.let { stream ->
                                        noteViewModel.importFromXml(stream)
                                    }
                            } catch (e: FileNotFoundException) {
                                // Show an error message in the log and in a snackbar
                                Log.e("MainActivity", e.stackTraceToString())

                                snackbarMessage?.value =
                                    resources.getString(R.string.error_import_xml_reading_file)
                                showSnackbar?.value = true
                            }
                        }
                    }
                }
            }

        val quitApplication: () -> Unit = { finish() }

        noteViewModel.syncUiState()

        setContent {
            val exportToXml: () -> Unit = {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val second = calendar.get(Calendar.SECOND)

                createDocument.launch(
                    "notes_$year-$month-$day-$hour-$minute-$second.txt"
                )
            }
            val importFromXml: () -> Unit = {
                readDocument.launch(arrayOf("text/plain"))
            }
            val snackbarHostState = remember { SnackbarHostState() }
            showSnackbar = rememberSaveable { mutableStateOf(false) }
            snackbarMessage = rememberSaveable { mutableStateOf("") }

            if (showSnackbar!!.value) {
                LaunchedEffect(snackbarHostState) {
                    snackbarHostState.showSnackbar(snackbarMessage!!.value)
                    showSnackbar!!.value = false
                }
            }

            NoteApp(
                quitApplication = quitApplication,
                exportToXml = exportToXml,
                importFromXml = importFromXml,
                noteViewModel = noteViewModel,
                snackbarHostState = snackbarHostState,
            )
        }
    }

    companion object {
        private var bluetoothConnectionManager: BluetoothConnectionManager? = null
    }
}