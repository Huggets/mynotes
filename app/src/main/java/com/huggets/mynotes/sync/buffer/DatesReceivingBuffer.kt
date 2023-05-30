package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header

class DatesReceivingBuffer(buffer: RemoteDataBuffer) : ReceivingBuffer<Pair<Date, Date>>(buffer) {
    override val fetchedData: List<Pair<Date, Date>>
        get() = remoteCreationDates

    private val remoteCreationDates = mutableListOf<Pair<Date, Date>>()

    override fun readBuffer() {
        buffer.skip(1) // Skip the header
        buffer.fetchMoreDataIfNeeded(1)

        val fetchedDates = mutableListOf<Pair<Date, Date>>()
        val datesToFetch = buffer.getUByte().toInt()

        while (fetchedDates.size != datesToFetch) {
            buffer.fetchMoreDataIfNeeded(DataSynchronizer.DATE_SIZE * 2)

            val creationDate = buffer.getDate()
            val modificationDate = buffer.getDate()

            fetchedDates.add(Pair(creationDate, modificationDate))
        }

        remoteCreationDates.addAll(fetchedDates)

        buffer.sendBytes(dateReceivedBuffer, 0, dateReceivedBuffer.size)
    }

    companion object {
        private val dateReceivedBuffer = ByteArray(1) { Header.DATES_BUFFER_RECEIVED.value }
    }
}