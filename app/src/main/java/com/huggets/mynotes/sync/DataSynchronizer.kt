package com.huggets.mynotes.sync

import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.data.DeletedNoteRepository
import com.huggets.mynotes.data.NoteAssociationRepository
import com.huggets.mynotes.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataSynchronizer(
    private val bluetoothConnectionManager: BluetoothConnectionManager,
    private val noteRepository: NoteRepository,
    private val noteAssociationRepository: NoteAssociationRepository,
    private val deletedNoteRepository: DeletedNoteRepository,
) {
    private var isSynchronizing = false
    private var sendingFinished = false
    private var receivingFinished = false

    private fun fetchData(buffer: ByteArray, offset: Int, length: Int): Int {
        return bluetoothConnectionManager.readData(buffer, offset, length)
    }

    private fun sendData(buffer: ByteArray, offset: Int, length: Int) {
        bluetoothConnectionManager.writeData(buffer, offset, length)
    }

    fun sync(onSynchronizationFinished: (Exception?) -> Unit) {
        if (isSynchronizing) {
            return
        }

        isSynchronizing = true
        sendingFinished = false
        receivingFinished = false

        CoroutineScope(Dispatchers.IO).launch {
            val notes = noteRepository.getAllNotes()
            val noteAssociations = noteAssociationRepository.getAllAssociations()
            val deletedNotes = deletedNoteRepository.getAllDeletedNotes()

            val sharedData =
                SharedData(notes, noteAssociations, deletedNotes, ::fetchData, ::sendData)

            val sender = DataSender(sharedData)
            val receiver = DataReceiver(sharedData)

            val onSenderFinished: (Exception?) -> Unit = {
                sendingFinished = true
                onSenderOrReceiverFinished(sharedData, it, onSynchronizationFinished)
            }
            val onReceiverFinished: (Exception?) -> Unit = {
                receivingFinished = true
                onSenderOrReceiverFinished(sharedData, it, onSynchronizationFinished)
            }

            sender.start(this, onSenderFinished)
            receiver.start(this, onReceiverFinished)
        }
    }

    private fun onSenderOrReceiverFinished(
        sharedData: SharedData,
        exception: Exception?,
        onSynchronizationFinished: (Exception?) -> Unit
    ) {
        if (sendingFinished && receivingFinished) {
            if (exception == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    updateRepository(sharedData)

                    isSynchronizing = false
                    onSynchronizationFinished(null)
                }
            } else {
                isSynchronizing = false
                onSynchronizationFinished(exception)
            }
        }
    }

    private suspend fun updateRepository(sharedData: SharedData) {
        sharedData.requestedNoteReceiver.obtain().forEach {
            noteRepository.insert(it)
        }
    }
}