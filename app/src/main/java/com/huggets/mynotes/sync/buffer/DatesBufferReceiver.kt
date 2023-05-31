package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.Header

/**
 * Receiver for the dates of the notes.
 *
 * @param buffer The buffer used to receive the data.
 */
class DatesBufferReceiver(buffer: ReceivingBuffer) : BufferReceiver<Pair<Date, Date>>(buffer) {
    override val fetchedData: List<Pair<Date, Date>>
        get() = remoteCreationDates

    /**
     * The dates that were received from the remote device.
     */
    private val remoteCreationDates = mutableListOf<Pair<Date, Date>>()

    override fun readBuffer() {
        buffer.skip(1) // Skip the header
        buffer.fetchMoreDataIfNeeded(1)

        val fetchedDates = mutableListOf<Pair<Date, Date>>()
        val datesToFetch = buffer.getUByte().toInt()

        while (fetchedDates.size != datesToFetch) {
            buffer.fetchMoreDataIfNeeded(Constants.DATE_SIZE * 2)

            val creationDate = buffer.getDate()
            val modificationDate = buffer.getDate()

            fetchedDates.add(Pair(creationDate, modificationDate))
        }

        remoteCreationDates.addAll(fetchedDates)

        buffer.sendBytes(confirmationBuffer, 0, confirmationBuffer.size)
    }

    companion object {
        /**
         * The buffer used to confirm the reception of the dates.
         */
        private val confirmationBuffer = ByteArray(1) { Header.DATES_BUFFER_RECEIVED.value }
    }
}