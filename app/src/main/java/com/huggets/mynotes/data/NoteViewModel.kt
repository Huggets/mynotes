package com.huggets.mynotes.data

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Xml
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.huggets.mynotes.R
import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.FetchData
import com.huggets.mynotes.sync.SendData
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteAssociationItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.OutputStream

class NoteViewModel(
    context: Context,
    private val bluetoothConnectionManager: BluetoothConnectionManager,
    private val resources: Resources,
) : ViewModel() {

    private val noteRepository = NoteRepository(context)
    private val noteAssociationRepository = NoteAssociationRepository(context)
    private val deletedNoteRepository = DeletedNoteRepository(context)

    private val _uiState = MutableStateFlow(NoteAppUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val updateBluetoothState = {
            _uiState.value = _uiState.value.copy(
                synchronizationState = _uiState.value.synchronizationState.copy(
                    bluetoothEnabled = bluetoothConnectionManager.bluetoothEnabled,
                    bluetoothPermissionGranted = bluetoothConnectionManager.isBluetoothPermissionGranted(),
                )
            )
        }
        bluetoothConnectionManager.setOnBluetoothActivationRequestDeniedCallback {
            updateBluetoothState.invoke()
        }
        bluetoothConnectionManager.setOnBluetoothActivationRequestAcceptedCallback {
            updateBluetoothState.invoke()
            updateBondedBluetoothDevices()
        }
        bluetoothConnectionManager.setOnBluetoothPermissionDeniedCallback {
            updateBluetoothState.invoke()
        }
        bluetoothConnectionManager.setOnBluetoothPermissionGrantedCallback {
            updateBluetoothState.invoke()
            bluetoothConnectionManager.requestBluetoothActivation()
        }

        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                bluetoothSupported = bluetoothConnectionManager.isBluetoothSupported(),
                bluetoothPermissionGranted = bluetoothConnectionManager.isBluetoothPermissionGranted(),
                bluetoothEnabled = bluetoothConnectionManager.bluetoothEnabled,
            )
        )
    }

    fun syncUiState() {
        viewModelScope.launch {
            noteRepository.syncAllNotes().collect {
                val list = mutableListOf<NoteItemUiState>()

                it.forEach { note ->
                    list.add(NoteItemUiState(note))
                }

                _uiState.value = _uiState.value.copy(allNotes = list)
            }
        }

        viewModelScope.launch {
            noteRepository.syncMainNotes().collect {
                val list = mutableListOf<Date>()

                it.forEach { creationDate ->
                    list.add(creationDate)
                }

                _uiState.value = _uiState.value.copy(mainNoteCreationDates = list)
            }
        }

        viewModelScope.launch {
            noteAssociationRepository.syncAllAssociations().collect {
                val list = mutableListOf<NoteAssociationItemUiState>()

                it.forEach { noteAssociation ->
                    list.add(NoteAssociationItemUiState(noteAssociation))
                }

                _uiState.value = _uiState.value.copy(noteAssociations = list)
            }
        }
    }

    /**
     * Update the given note in the database.
     */
    fun updateNote(note: NoteItemUiState, onNoteUpdated: () -> Unit) {
        viewModelScope.launch {
            noteRepository.update(note.toNote())
            onNoteUpdated()
        }
    }

    /**
     * Start the creation of a new note in the database.
     *
     * If a parent note's creation date is not null, the note will be associated with this parent note.
     *
     * @param parentCreationDate The creation date of the parent note, if any.
     * @param onCreationDone Callback called when the creation is done.
     */
    fun createNote(parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) {
        viewModelScope.launch {
            val currentDate = Date.getCurrentTime()
            val newNote = Note("", "", currentDate, currentDate)

            noteRepository.insert(newNote)
            associateNote(parentCreationDate, currentDate)

            onCreationDone(currentDate)
        }
    }

    /**
     * Associate a note (given by its creation date) with a parent note (given by its creation date).
     *
     * @param parentCreationDate The creation date of the parent note. If null, the note will not be
     * associated with any parent note.
     * @param creationDate The creation date of the note to associate.
     */
    private suspend fun associateNote(parentCreationDate: Date?, creationDate: Date) {
        if (parentCreationDate != null) {
            val noteAssociation = NoteAssociationItemUiState(parentCreationDate, creationDate)
                .toNoteAssociation()

            noteAssociationRepository.insert(noteAssociation)
        }
    }

    /**
     * Deletes a note (by its creation date) and all its children in the database.
     */
    fun deleteNote(creationDate: Date) {
        viewModelScope.launch {
            noteRepository.getChildren(creationDate).forEach { child ->
                noteRepository.delete(child.creationDate)
                deletedNoteRepository.insert(DeletedNote(child.creationDate))
                noteAssociationRepository.deleteByChildCreationDate(child.creationDate)
            }

            noteRepository.delete(creationDate)
            deletedNoteRepository.insert(DeletedNote(creationDate))
        }
    }

    /**
     * Open a document for writing and export the data to it.
     *
     * @param openDocument The lambda to open the document. When the lambda is finished, it must
     * call [onExportedFileOpened].
     */
    fun export(openDocument: () -> Unit) {
        _uiState.value =
            _uiState.value.copy(isExporting = true, exportFailed = false, exportFailedMessage = "")

        openDocument()
    }

    /**
     * Exports the data to a file if the user chose a file to export to.
     *
     * @param stream The stream to write the data to. If null and [fileChosen] is true, the export
     * failed.
     * @param fileChosen Whether the user chose a file to export to.
     */
    fun onExportedFileOpened(stream: OutputStream?, fileChosen: Boolean) {
        if (!fileChosen) {
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportFailed = false,
                exportFailedMessage = "",
            )
        } else if (stream == null) {
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportFailed = true,
                exportFailedMessage = resources.getString(R.string.error_export_xml_writing_file)
            )
        } else {
            dataToXml(stream)
        }
    }

    /**
     * Exports the data to XML.
     *
     * @param stream The stream to write the data to.
     */
    private fun dataToXml(stream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val serializer = Xml.newSerializer()
            serializer.setOutput(stream, "UTF-8")
            serializer.startDocument("UTF-8", true)

            serializer.startTag("", "data")
            serializer.attribute("", "version", "2")
            notesToXml(serializer)
            noteAssociationsToXml(serializer)
            deletedNotesToXml(serializer)
            serializer.endTag("", "data")

            serializer.endDocument()
            stream.close()
        }
    }

    /**
     * Converts all the notes to XML.
     *
     * @param serializer The serializer to use.
     */
    private suspend fun notesToXml(serializer: XmlSerializer) {
        serializer.startTag("", "notes")

        noteRepository.getAllNotes().forEach { note ->
            serializer.startTag("", "note")
            serializer.attribute("", "title", note.title)
            serializer.attribute("", "content", note.content)
            serializer.attribute("", "creationDate", note.creationDate.toString())
            serializer.attribute("", "lastEditTime", note.lastEditTime.toString())
            serializer.endTag("", "note")
        }

        serializer.endTag("", "notes")
    }

    /**
     * Converts all the note associations to XML.
     *
     * @param serializer The serializer to use.
     */
    private suspend fun noteAssociationsToXml(serializer: XmlSerializer) {
        serializer.startTag("", "noteAssociations")

        noteAssociationRepository.getAllAssociations().forEach { noteAssociation ->
            serializer.startTag("", "noteAssociation")
            serializer.attribute(
                "",
                "parentCreationDate",
                noteAssociation.parentCreationDate.toString()
            )
            serializer.attribute(
                "",
                "childCreationDate",
                noteAssociation.childCreationDate.toString()
            )
            serializer.endTag("", "noteAssociation")
        }

        serializer.endTag("", "noteAssociations")
    }

    /**
     * Converts all the deleted notes to XML.
     */
    private suspend fun deletedNotesToXml(serializer: XmlSerializer) {
        serializer.startTag("", "deletedNotes")

        deletedNoteRepository.getAllDeletedNotes().forEach { deletedNote ->
            serializer.startTag("", "deletedNote")
            serializer.attribute(
                "",
                "creationDate",
                deletedNote.creationDate.toString()
            )
            serializer.endTag("", "deletedNote")
        }

        serializer.endTag("", "deletedNotes")
    }

    /**
     * Open a document for reading and import the data from it.
     *
     * @param openDocument The lambda to open the document. When the lambda is finished, it must
     * call [onImportedFileOpened].
     */
    fun import(openDocument: () -> Unit) {
        _uiState.value =
            _uiState.value.copy(isImporting = true, importFailed = false, importFailedMessage = "")

        openDocument()
    }

    /**
     * Imports the data from a file if the user chose a file to import from.
     *
     * @param stream The stream to read the data from. If null and [fileChosen] is true, the import
     * failed.
     */
    fun onImportedFileOpened(stream: InputStream?, fileChosen: Boolean) {
        if (!fileChosen) {
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                importFailed = false,
                importFailedMessage = "",
            )
        } else if (stream == null) {
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                importFailed = true,
                importFailedMessage = resources.getString(R.string.error_import_xml_reading_file)
            )
        } else {
            dataFromXml(stream)
        }
    }

    /**
     * Imports the data from XML.
     *
     * @param stream The stream to read the data from.
     */
    private fun dataFromXml(stream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")

            val importedNotes = mutableListOf<Note>()
            val importedNoteAssociations = mutableListOf<NoteAssociation>()
            val importedDeletedNotes = mutableListOf<DeletedNote>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "data" -> {
                            val version: Int? = parser.getAttributeValue("", "version")?.toInt()
                            if (version != 2) {
                                stream.close()
                                _uiState.value = _uiState.value.copy(
                                    isImporting = false,
                                    importFailed = true,
                                    importFailedMessage = resources.getString(R.string.unsupported_version)
                                )
                                return@launch
                            }
                        }

                        "note" -> {
                            val title = parser.getAttributeValue("", "title")
                            val content = parser.getAttributeValue("", "content")
                            val creationDate = Date.fromString(
                                parser.getAttributeValue("", "creationDate")
                            )
                            val lastEditTime = Date.fromString(
                                parser.getAttributeValue("", "lastEditTime")
                            )

                            importedNotes.add(Note(title, content, creationDate, lastEditTime))
                        }

                        "noteAssociation" -> {
                            val parentCreationDate = Date.fromString(
                                parser.getAttributeValue("", "parentCreationDate")
                            )
                            val childCreationDate = Date.fromString(
                                parser.getAttributeValue("", "childCreationDate")
                            )

                            importedNoteAssociations.add(
                                NoteAssociation(parentCreationDate, childCreationDate)
                            )
                        }

                        "deletedNote" -> {
                            val creationDate = Date.fromString(
                                parser.getAttributeValue("", "creationDate")
                            )

                            importedDeletedNotes.add(DeletedNote(creationDate))
                        }
                    }
                }
                eventType = parser.next()
            }

            stream.close()

            importedNotes.forEach {
                if (noteRepository.get(it.creationDate) == null) {
                    noteRepository.insert(it)
                }
            }
            noteAssociationRepository.insert(*importedNoteAssociations.toTypedArray())
            deletedNoteRepository.insert(*importedDeletedNotes.toTypedArray())

            importedDeletedNotes.forEach { deletedNote ->
                noteRepository.delete(deletedNote.creationDate)
            }

            noteRepository.getAllNotes().forEach {
                deletedNoteRepository.delete(it.creationDate)
            }

            _uiState.value = _uiState.value.copy(isImporting = false)
        }
    }

    /**
     * Updates the list of bonded Bluetooth devices.
     */
    private fun updateBondedBluetoothDevices() {
        val bondedDevices = mutableMapOf<String, String>().apply {
            bluetoothConnectionManager.getBondedDevices().forEach {
                try {
                    put(it.address, it.name)
                } catch (e: SecurityException) {
                    // This happens when the user has revoked the location permission
                    // for the app. In this case, we can't get the name of the device.
                    put(it.address, it.address)
                }
            }
        }
        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                bondedDevices = bondedDevices
            )
        )
    }

    /**
     * Enables Bluetooth if it is supported and not enabled yet.
     *
     * If the user has not granted the permission to use Bluetooth, it will be requested.
     */
    fun enableBluetooth() {
        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                bluetoothEnabled = bluetoothConnectionManager.bluetoothEnabled,
                synchronisationError = false,
                synchronisationErrorMessage = "",
            )
        )

        if (!bluetoothConnectionManager.isBluetoothSupported()) {
            return
        }

        if (!bluetoothConnectionManager.isBluetoothPermissionGranted()) {
            bluetoothConnectionManager.requestBluetoothPermission()
        } else if (!bluetoothConnectionManager.bluetoothEnabled) {
            bluetoothConnectionManager.requestBluetoothActivation()
        } else {
            updateBondedBluetoothDevices()
        }
    }

    /**
     * Stops any ongoing Bluetooth connection.
     */
    fun cancelBluetoothConnection() {
        bluetoothConnectionManager.stopConnection()
        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                connecting = false,
                connected = false,
            )
        )
    }

    /**
     * Connects to the Bluetooth device with the given address.
     *
     * @param deviceAddress The address of the device to connect to.
     */
    fun connectToBluetoothDevice(deviceAddress: String) {
        val device = bluetoothConnectionManager.getBondedDevices().find {
            it.address.equals(deviceAddress)
        } ?: return

        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                connecting = true,
                synchronisationError = false,
                synchronisationErrorMessage = "",
            )
        )

        bluetoothConnectionManager.connect(
            device,
            ::onBluetoothConnectionEstablished,
            ::onBluetoothConnectionError,
        )
    }

    /**
     * Called when a Bluetooth connection is established.
     *
     * It starts the data synchronization process.
     */
    private fun onBluetoothConnectionEstablished() {
        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                connecting = false,
                connected = true,
            )
        )

        val fetchData: FetchData = { buffer, offset, length ->
            bluetoothConnectionManager.readData(buffer, offset, length)
        }
        val sendData: SendData = { buffer, offset, length ->
            bluetoothConnectionManager.sendData(buffer, offset, length)
        }

        val dataSynchronizer = DataSynchronizer(
            fetchData,
            sendData,
            noteRepository,
            noteAssociationRepository,
            deletedNoteRepository,
        )
        dataSynchronizer.sync {
            bluetoothConnectionManager.stopConnection()

            val error = it != null
            val errorMessage = it?.message ?: resources.getString(R.string.no_message)

            _uiState.value = _uiState.value.copy(
                synchronizationState = _uiState.value.synchronizationState.copy(
                    connected = false,
                    synchronisationError = error,
                    synchronisationErrorMessage = errorMessage,
                )
            )
        }
    }

    /**
     * Called when a Bluetooth connection error occurs.
     *
     * @param exception The exception that caused the error.
     */
    private fun onBluetoothConnectionError(exception: Exception) {
        _uiState.value = _uiState.value.copy(
            synchronizationState = _uiState.value.synchronizationState.copy(
                connecting = false,
                connected = false,
                synchronisationError = true,
                synchronisationErrorMessage = exception.message
                    ?: resources.getString(R.string.no_message)
            )
        )
    }

    companion object {
        val APPLICATION_KEY_EXTRAS = object : CreationExtras.Key<Application> {}
        val BLUETOOTH_CONNECTION_MANAGER_KEY_EXTRAS =
            object : CreationExtras.Key<BluetoothConnectionManager> {}
        val RESOURCES_KEY_EXTRAS = object : CreationExtras.Key<Resources> {}

        @Suppress("UNCHECKED_CAST")
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY_EXTRAS])
                val bluetoothConnectionManager =
                    checkNotNull(extras[BLUETOOTH_CONNECTION_MANAGER_KEY_EXTRAS])
                val resources = checkNotNull(extras[RESOURCES_KEY_EXTRAS])

                return NoteViewModel(application, bluetoothConnectionManager, resources) as? T
                    ?: throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}