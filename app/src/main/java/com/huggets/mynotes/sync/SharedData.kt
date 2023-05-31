package com.huggets.mynotes.sync

import com.huggets.mynotes.data.DeletedNote
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.data.NoteAssociation
import com.huggets.mynotes.sync.buffer.AssociationsBufferReceiver
import com.huggets.mynotes.sync.buffer.AssociationsBufferSender
import com.huggets.mynotes.sync.buffer.DatesBufferReceiver
import com.huggets.mynotes.sync.buffer.DatesBufferSender
import com.huggets.mynotes.sync.buffer.DeletedNotesReceiver
import com.huggets.mynotes.sync.buffer.DeletedNotesSender
import com.huggets.mynotes.sync.buffer.NeededNotesBufferReceiver
import com.huggets.mynotes.sync.buffer.NeededNotesBufferSender
import com.huggets.mynotes.sync.buffer.ReceivingBuffer
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferReceiver
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender
import com.huggets.mynotes.sync.buffer.SendingBuffer

/**
 * The data shared between a [DataSender] and a [DataReceiver].
 *
 * It includes the notes, the note associations and the deleted notes.
 *
 * @property notes The notes of this device.
 * @property noteAssociations The note associations of this device.
 * @property deletedNotes The deleted notes of this device.
 * @param fetchData The function used to fetch data from the other device.
 * @property sendData The function used to send data to the other device.
 */
class SharedData(
    val notes: List<Note>,
    val noteAssociations: List<NoteAssociation>,
    val deletedNotes: List<DeletedNote>,
    fetchData: FetchData,
    val sendData: SendData,
) {
    /**
     * The buffer used to receive data.
     */
    private val receivingBuffer = ReceivingBuffer(ByteArray(8096), fetchData, sendData)

    /**
     * The buffer used to send data.
     */
    private val sendingBuffer = SendingBuffer(ByteArray(8096), sendData)

    /**
     * The dates receiver.
     */
    val datesReceiver = DatesBufferReceiver(receivingBuffer)

    /**
     * The needed notes receiver.
     */
    val neededNotesReceiver = NeededNotesBufferReceiver(receivingBuffer)

    /**
     * The requested note receiver.
     */
    val requestedNoteReceiver = RequestedNoteBufferReceiver(receivingBuffer)

    /**
     * The associations receiver.
     */
    val associationsReceiver = AssociationsBufferReceiver(receivingBuffer)

    /**
     * The deleted notes receiver.
     */
    val deletedNotesReceiver = DeletedNotesReceiver(receivingBuffer)

    /**
     * The dates sender.
     */
    val datesSender = DatesBufferSender(sendingBuffer)

    /**
     * The needed notes sender.
     */
    val neededNotesSender = NeededNotesBufferSender(sendingBuffer)

    /**
     * The requested note sender.
     */
    val requestedNoteSender = RequestedNoteBufferSender(sendingBuffer)

    /**
     * The associations sender.
     */
    val associationsSender = AssociationsBufferSender(sendingBuffer)

    /**
     * The deleted notes sender.
     */
    val deletedNotesSender = DeletedNotesSender(sendingBuffer)

    /**
     * The data end sender.
     */
    val dataEndSender = DataEndSender(sendData)

    /**
     * The number of bytes fetched during the last fetch.
     */
    val bytesFetched: Int
        get() = receivingBuffer.bytesFetched

    /**
     * The current byte the receiver is looking at.
     */
    val currentByte: Byte
        get() = receivingBuffer.lookByte()

    /**
     * The number of bytes the receiver can read.
     */
    val availableBytes: Int
        get() = receivingBuffer.bytesFetchedAvailable()

    /**
     * Stops the senders.
     */
    fun stop() {
        datesSender.stop()
        neededNotesSender.stop()
        requestedNoteSender.stop()
        associationsSender.stop()
        deletedNotesSender.stop()
        dataEndSender.stop()
    }

    /**
     * Fetches new data when no more data is available in the receiving buffer.
     */
    fun fetchData() {
        receivingBuffer.fetchDataFromStart()
    }

    /**
     * Skips the current byte in the receiving buffer and moves to the next one.
     */
    fun nextByte() {
        receivingBuffer.skip(1)
    }
}