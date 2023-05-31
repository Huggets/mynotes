package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.Header

/**
 * Receiver for the needed notes.
 *
 * @param buffer The buffer used to receive the data.
 */
class NeededNotesBufferReceiver(buffer: ReceivingBuffer) : BufferReceiver<Date>(buffer) {

    override val fetchedData: List<Date>
        get() = neededNoteDates

    /**
     * The dates of the notes that are needed.
     */
    private val neededNoteDates = mutableListOf<Date>()

    override fun readBuffer() {
        buffer.skip(1) // Skip the header
        buffer.fetchMoreDataIfNeeded(1)

        val fetchedDates = mutableListOf<Date>()
        val datesToFetch = buffer.getUByte().toInt()

        while (fetchedDates.size != datesToFetch) {
            buffer.fetchMoreDataIfNeeded(Constants.DATE_SIZE)

            fetchedDates.add(buffer.getDate())
        }

        neededNoteDates.addAll(fetchedDates)

        buffer.sendBytes(confirmationBytes, 0, confirmationBytes.size)
    }

    companion object {
        /**
         * The buffer used to confirm the reception of the needed notes.
         */
        private val confirmationBytes = ByteArray(1) { Header.NEEDED_NOTES_BUFFER_RECEIVED.value }
    }
}