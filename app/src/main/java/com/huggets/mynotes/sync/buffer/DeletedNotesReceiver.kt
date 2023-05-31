package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.DeletedNote
import com.huggets.mynotes.sync.Header

/**
 * Receiver for the deleted notes that this device needs.
 *
 * @param buffer The buffer used to receive the data.
 */
class DeletedNotesReceiver(buffer: ReceivingBuffer) :
    BufferReceiver<DeletedNote>(buffer) {

    override val fetchedData: List<DeletedNote>
        get() = remoteDeletedNotes

    /**
     * The deleted notes that this device needs.
     */
    private val remoteDeletedNotes = mutableListOf<DeletedNote>()

    override fun readBuffer() {
        buffer.skip(1) // Skip the header
        buffer.fetchMoreDataIfNeeded(1)

        val fetchedAssociations = mutableListOf<DeletedNote>()
        val associationsToFetch = buffer.getUByte().toInt()

        while (fetchedAssociations.size != associationsToFetch) {
            buffer.fetchMoreDataIfNeeded(Constants.DATE_SIZE)

            val creationDate = buffer.getDate()

            fetchedAssociations.add(DeletedNote(creationDate))
        }

        remoteDeletedNotes.addAll(fetchedAssociations)

        buffer.sendBytes(confirmationBytes, 0, confirmationBytes.size)
    }

    companion object {
        /**
         * The bytes that are sent to the sender to confirm that the associations were received.
         */
        private val confirmationBytes = ByteArray(1) { Header.DELETED_NOTES_BUFFER_RECEIVED.value }
    }
}
