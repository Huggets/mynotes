package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.NoteAssociation
import com.huggets.mynotes.sync.Header

/**
 * Receiver for the associations that this device needs.
 *
 * @param buffer The buffer used to receive the data.
 */
class AssociationsBufferReceiver(buffer: ReceivingBuffer) :
    BufferReceiver<NoteAssociation>(buffer) {

    override val fetchedData: List<NoteAssociation>
        get() = remoteAssociations

    /**
     * The associations that this device needs.
     */
    private val remoteAssociations = mutableListOf<NoteAssociation>()

    override fun readBuffer() {
        buffer.skip(1) // Skip the header
        buffer.fetchMoreDataIfNeeded(1)

        val fetchedAssociations = mutableListOf<NoteAssociation>()
        val associationsToFetch = buffer.getUByte().toInt()

        while (fetchedAssociations.size != associationsToFetch) {
            buffer.fetchMoreDataIfNeeded(Constants.DATE_SIZE * 2)

            val parent = buffer.getDate()
            val child = buffer.getDate()

            fetchedAssociations.add(NoteAssociation(parent, child))
        }

        remoteAssociations.addAll(fetchedAssociations)

        buffer.sendBytes(confirmationBytes, 0, confirmationBytes.size)
    }

    companion object {
        /**
         * The bytes that are sent to the sender to confirm that the associations were received.
         */
        private val confirmationBytes = ByteArray(1) { Header.ASSOCIATIONS_BUFFER_RECEIVED.value }
    }
}
