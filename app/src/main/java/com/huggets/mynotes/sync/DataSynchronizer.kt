package com.huggets.mynotes.sync

import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.data.Date
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
                SharedData(notes, noteAssociations, deletedNotes, bluetoothConnectionManager)

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
        sharedData.requestedNoteReceivingBuffer.obtain().forEach {
            noteRepository.insert(it)
        }
    }

    companion object {
        enum class Header(val value: Byte) {
            DATA_END(0x01),
            DATA_END_RECEIVED(0x02),

            DATES(0x020),
            DATES_COUNT(0x21),
            DATES_BUFFER_RECEIVED(0x22),

            NEEDED_NOTE(0x30),
            NEEDED_NOTES_COUNT(0x31),
            NEEDED_NOTE_BUFFER_RECEIVED(0x32),

            REQUESTED_NOTES(0x40),
            REQUESTED_NOTES_COUNT(0x41),
            REQUESTED_NOTES_BUFFER_RECEIVED(0x42),
            REQUESTED_NOTES_BUFFER_END(0x43),
            REQUESTED_NOTES_TITLE_LENGTH(0x44),
            REQUESTED_NOTES_CONTENT_LENGTH(0x45),
            REQUESTED_NOTES_CREATION_DATE(0x46),
            REQUESTED_NOTES_LAST_MODIFICATION_DATE(0x47),
        }

        const val DATE_SIZE = 28

        fun ByteArray.addInt(value: Int, offset: Int) {
            this[offset] = (value shr 24).toByte()
            this[offset + 1] = (value shr 16).toByte()
            this[offset + 2] = (value shr 8).toByte()
            this[offset + 3] = value.toByte()
        }

        fun ByteArray.addDate(date: Date, offset: Int) {
            date.apply {
                addInt(year, offset)
                addInt(month, offset + 4)
                addInt(day, offset + 8)
                addInt(hour, offset + 12)
                addInt(minute, offset + 16)
                addInt(second, offset + 20)
                addInt(millisecond, offset + 24)
            }
        }
    }
}