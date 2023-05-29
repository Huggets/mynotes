package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray

class DatesReceivingBuffer(
    fetch: (ByteArray, Int, Int) -> Int,
    send: (ByteArray, Int, Int) -> Unit,
) : ReceivingBuffer<Pair<Date, Date>>(fetch, send) {

    override val fetchedData: List<Pair<Date, Date>>
        get() = remoteCreationDates

    private val remoteCreationDates = mutableListOf<Pair<Date, Date>>()

    override fun readBuffer(
        buffer: ByteArray,
        bufferIndex: Int,
        bufferMaxIndex: Int
    ): Pair<Int, Int> {

        var index = bufferIndex
        var maxIndex = bufferMaxIndex

        index++

        fetchMoreDataIfNeeded(buffer, index, maxIndex, 1).apply {
            index = first
            maxIndex = second
        }

        val dateCount = buffer[index].toUByte().toInt()
        index += 1
        val fetchedDates = mutableListOf<Pair<Date, Date>>()

        while (fetchedDates.size != dateCount) {
            fetchMoreDataIfNeeded(buffer, index, maxIndex, DataSynchronizer.DATE_SIZE * 2).apply {
                index = first
                maxIndex = second
            }

            val creationDate = Date.fromByteArray(buffer, index)
            index += DataSynchronizer.DATE_SIZE
            val modificationDate = Date.fromByteArray(buffer, index)
            index += DataSynchronizer.DATE_SIZE

            fetchedDates.add(Pair(creationDate, modificationDate))
        }

        remoteCreationDates.addAll(fetchedDates)

        sendData(dateReceivedBuffer, 0, dateReceivedBuffer.size)

        return Pair(index, maxIndex)
    }

    companion object {
        private val dateReceivedBuffer = ByteArray(1) { Header.DATES_BUFFER_RECEIVED.value }
    }
}