package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.Header

class NeededNotesBufferReceiver(buffer: ReceivingBuffer) : BufferReceiver<Date>(buffer) {

    override val fetchedData: List<Date>
        get() = neededNoteDates

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

        buffer.sendBytes(confirmationBuffer, 0, confirmationBuffer.size)
    }

    companion object {
        private val confirmationBuffer = ByteArray(1) { Header.NEEDED_NOTES_BUFFER_RECEIVED.value }
    }
}