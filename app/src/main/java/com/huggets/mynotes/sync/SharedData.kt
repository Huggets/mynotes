package com.huggets.mynotes.sync

import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.data.DeletedNote
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.data.NoteAssociation
import com.huggets.mynotes.sync.buffer.DatesReceivingBuffer
import com.huggets.mynotes.sync.buffer.DatesSendingBuffer
import com.huggets.mynotes.sync.buffer.NeededNotesReceivingBuffer
import com.huggets.mynotes.sync.buffer.NeededNotesSendingBuffer
import com.huggets.mynotes.sync.buffer.RemoteDataBuffer
import com.huggets.mynotes.sync.buffer.RequestedNoteReceivingBuffer
import com.huggets.mynotes.sync.buffer.RequestedNoteSendingBuffer
import kotlinx.coroutines.channels.Channel

class SharedData(
    val notes: List<Note>,
    val noteAssociations: List<NoteAssociation>,
    val deletedNotes: List<DeletedNote>,
    val bluetoothConnectionManager: BluetoothConnectionManager,
) {
    val receivingBuffer: RemoteDataBuffer

    val datesReceivingBuffer: DatesReceivingBuffer
    val neededNotesReceivingBuffer: NeededNotesReceivingBuffer
    val requestedNoteReceivingBuffer: RequestedNoteReceivingBuffer

    val datesSendingBuffer: DatesSendingBuffer
    val neededNotesSendingBuffer: NeededNotesSendingBuffer
    val requestedNoteSendingBuffer: RequestedNoteSendingBuffer

    val datesSendingChannel = Channel<Unit>(Channel.UNLIMITED)
    val neededNotesChannel = Channel<Unit>(Channel.UNLIMITED)
    val requestedNoteChannel = Channel<Unit>(Channel.UNLIMITED)
    val dataEndChannel = Channel<Unit>(Channel.UNLIMITED)

    var hasOtherDeviceSentEverything = false
    var hasReceivedAllRequestedNotes = false
    var hasOtherDeviceReceivedDataEnd = false

    init {
        val fetch: (ByteArray, Int, Int) -> Int = { buffer, bufferIndex, maxIndex ->
            bluetoothConnectionManager.readData(buffer, bufferIndex, maxIndex)
        }
        val send: (ByteArray, Int, Int) -> Unit = { buffer, bufferIndex, maxIndex ->
            bluetoothConnectionManager.writeData(buffer, bufferIndex, maxIndex)
        }
        receivingBuffer = RemoteDataBuffer(ByteArray(59), fetch, send)
        val sendingBuffer = ByteArray(58)

        datesReceivingBuffer = DatesReceivingBuffer(receivingBuffer)
        neededNotesReceivingBuffer = NeededNotesReceivingBuffer(receivingBuffer)
        requestedNoteReceivingBuffer = RequestedNoteReceivingBuffer(receivingBuffer)

        datesSendingBuffer = DatesSendingBuffer(sendingBuffer)
        neededNotesSendingBuffer = NeededNotesSendingBuffer(sendingBuffer)
        requestedNoteSendingBuffer = RequestedNoteSendingBuffer(sendingBuffer)
    }

    fun stop() {
        datesSendingChannel.close()
        neededNotesChannel.close()
        requestedNoteChannel.close()
        dataEndChannel.close()
    }
}