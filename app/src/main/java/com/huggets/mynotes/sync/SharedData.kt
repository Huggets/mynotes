package com.huggets.mynotes.sync

import com.huggets.mynotes.data.DeletedNote
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.data.NoteAssociation
import com.huggets.mynotes.sync.buffer.DatesBufferReceiver
import com.huggets.mynotes.sync.buffer.DatesBufferSender
import com.huggets.mynotes.sync.buffer.NeededNotesBufferReceiver
import com.huggets.mynotes.sync.buffer.NeededNotesBufferSender
import com.huggets.mynotes.sync.buffer.ReceivingBuffer
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferReceiver
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender
import com.huggets.mynotes.sync.buffer.SendingBuffer

typealias FetchData = (buffer: ByteArray, offset: Int, length: Int) -> Int
typealias SendData = (buffer: ByteArray, offset: Int, length: Int) -> Unit

class SharedData(
    val notes: List<Note>,
    val noteAssociations: List<NoteAssociation>,
    val deletedNotes: List<DeletedNote>,
    fetchData: FetchData,
    val sendData: SendData,
) {
    private val receivingBuffer = ReceivingBuffer(ByteArray(8096), fetchData, sendData)
    private val sendingBuffer = SendingBuffer(ByteArray(8096), sendData)

    val datesReceiver = DatesBufferReceiver(receivingBuffer)
    val neededNotesReceiver = NeededNotesBufferReceiver(receivingBuffer)
    val requestedNoteReceiver = RequestedNoteBufferReceiver(receivingBuffer)

    val datesSender = DatesBufferSender(sendingBuffer)
    val neededNotesSender = NeededNotesBufferSender(sendingBuffer)
    val requestedNoteSender = RequestedNoteBufferSender(sendingBuffer)
    val dataEndSender = DataEndSender(sendData)

    val bytesFetched: Int
        get() = receivingBuffer.bytesFetched

    val currentByte: Byte
        get() = receivingBuffer.lookByte()

    val availableBytes: Int
        get() = receivingBuffer.bytesFetchedAvailable()

    fun stop() {
        datesSender.close()
        neededNotesSender.close()
        requestedNoteSender.close()
        dataEndSender.close()
    }

    fun fetchData() {
        receivingBuffer.fetchDataFromStart()
    }

    fun nextByte() {
        receivingBuffer.skip(1)
    }
}